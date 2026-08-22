package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.component.ProfanityValidator;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.DeletePasswordEncoder;
import com.practicket.community.component.DeletePasswordPolicy;
import com.practicket.community.component.PostPasswordAttemptLimiter;
import com.practicket.community.component.PostRateLimiter;
import com.practicket.community.component.PostViewCounter;
import com.practicket.community.component.TagNormalizer;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostLike;
import com.practicket.community.domain.entity.PostTag;
import com.practicket.community.domain.repository.PostLikeRepository;
import com.practicket.community.domain.repository.PostTagRepository;
import com.practicket.community.domain.repository.PostQueryCondition;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final String ANONYMOUS_NICKNAME = "익명";

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostTagRepository postTagRepository;
    private final ClientManager clientManager;
    private final DeletePasswordEncoder deletePasswordEncoder;
    private final PostPasswordAttemptLimiter passwordAttemptLimiter;
    private final PostViewCounter viewCounter;
    private final PostRateLimiter postRateLimiter;
    private final ProfanityValidator profanityValidator;

    @Transactional
    public PostResponse create(PostCreateRequest request, ClientInfo clientInfo, String clientIp) {
        DeletePasswordPolicy.validate(request.getDeletePassword());

        Client client = clientManager.findById(clientInfo.getClientId());

        if (client.isBanned(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.POST_BANNED);
        }

        profanityValidator.validateProfanityText(request.getTitle());
        profanityValidator.validateProfanityText(request.getContent());

        // 검증을 통과한 요청만 센다. 거부된 글이 할당량을 먹으면 안 된다
        postRateLimiter.validate(clientInfo.getToken(), clientIp);

        Post post = Post.builder()
                .client(client)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .nickname(resolveNickname(client.getName()))
                .ip(clientIp)
                .deletePasswordHash(deletePasswordEncoder.encode(request.getDeletePassword()))
                .build();

        Post saved = postRepository.save(post);
        return PostResponse.from(saved, true, replaceTags(saved, request.getTags()));
    }

    public Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable) {
        PostQueryCondition queryCondition = PostQueryCondition.builder()
                .keyword(condition.getKeyword())
                .tag(TagNormalizer.normalizeOne(condition.getTag()))
                .sort(PostQueryCondition.PostSortType.from(condition.getSort()))
                .build();

        Page<Post> posts = postRepository.search(queryCondition, pageable);
        Map<Long, List<String>> tagsByPostId = findTagsOf(posts.getContent());

        return posts.map(post -> PostListResponse.from(
                post, tagsByPostId.getOrDefault(post.getId(), List.of())));
    }

    /** 이 브라우저(토큰)로 쓴 글. 기기를 바꾸면 목록이 빈다 */
    public Page<PostListResponse> searchMine(ClientInfo clientInfo, Pageable pageable) {
        PostQueryCondition queryCondition = PostQueryCondition.builder()
                .clientId(clientInfo.getClientId())
                .build();

        Page<Post> posts = postRepository.search(queryCondition, pageable);
        Map<Long, List<String>> tagsByPostId = findTagsOf(posts.getContent());

        return posts.map(post -> PostListResponse.from(
                post, tagsByPostId.getOrDefault(post.getId(), List.of())));
    }

    public PostResponse get(Long postId, ClientInfo clientInfo) {
        Post post = findActivePost(postId);
        return PostResponse.from(post, isWrittenBy(post, clientInfo), findTagsOf(post));
    }

    /** 조회수는 서버 렌더링이 아니라 여기서 센다 — 토큰이 localStorage 에 있어 렌더링 시점엔 중복을 못 거른다 */
    @Transactional
    public PostResponse getAndCountView(Long postId, ClientInfo clientInfo) {
        Post post = findActivePost(postId);

        if (clientInfo == null) {
            return PostResponse.from(post, false, false, findTagsOf(post));
        }

        boolean mine = isWrittenBy(post, clientInfo);
        boolean liked = postLikeRepository.existsByPostAndClient(post, clientManager.findById(clientInfo.getClientId()));

        if (viewCounter.markViewed(postId, clientInfo.getClientId())) {
            postRepository.incrementViewCount(postId);
            // 위 UPDATE 가 컨텍스트를 비웠으므로 다시 읽는다
            post = findActivePost(postId);
        }

        return PostResponse.from(post, mine, liked, findTagsOf(post));
    }

    /** 중복의 실제 방어선은 아래 if 가 아니라 {@code post_like} 의 유니크 제약이다 */
    @Transactional
    public PostLikeResponse toggleLike(Long postId, ClientInfo clientInfo) {
        Post post = findActivePost(postId);
        Client client = clientManager.findById(clientInfo.getClientId());

        // 스팸글을 쓰고 본인이 추천해 sitemap 색인 조건을 통과시키는 길을 막는다
        if (post.isWrittenBy(clientInfo.getClientId())) {
            throw new ValidateException(ErrorCode.POST_LIKE_SELF);
        }

        long likeCount = post.getLikeCount();
        boolean liked;

        if (postLikeRepository.existsByPostAndClient(post, client)) {
            postLikeRepository.deleteByPostAndClient(post, client);
            postRepository.decrementLikeCount(postId);
            liked = false;
            likeCount = Math.max(0L, likeCount - 1);
        } else {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .client(client)
                    .build());
            postRepository.incrementLikeCount(postId);
            liked = true;
            likeCount++;
        }

        return PostLikeResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    /** 토큰이 작성자일 때만 된다. 삭제 비밀번호로는 수정할 수 없다 */
    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest request, ClientInfo clientInfo) {
        Post post = findActivePost(postId);

        if (!isWrittenBy(post, clientInfo)) {
            throw new ValidateException(ErrorCode.POST_FORBIDDEN);
        }

        // 작성만 검사하면 깨끗하게 올린 뒤 수정으로 욕설을 넣어 우회할 수 있다.
        profanityValidator.validateProfanityText(request.getTitle());
        profanityValidator.validateProfanityText(request.getContent());
        post.update(request.getTitle().trim(), request.getContent());
        return PostResponse.from(post, true, replaceTags(post, request.getTags()));
    }

    @Transactional
    public void delete(Long postId, PostDeleteRequest request, ClientInfo clientInfo, String clientIp) {
        Post post = findActivePost(postId);

        if (!isWrittenBy(post, clientInfo)) {
            verifyDeletePassword(post, request, clientIp);
        }

        post.softDelete(LocalDateTime.now());
    }

    private void verifyDeletePassword(Post post, PostDeleteRequest request, String clientIp) {
        String rawPassword = request == null ? null : request.getDeletePassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidateException(ErrorCode.POST_PASSWORD_REQUIRED);
        }

        passwordAttemptLimiter.validateNotLocked(post.getId(), clientIp);

        if (!deletePasswordEncoder.matches(rawPassword, post.getDeletePasswordHash())) {
            passwordAttemptLimiter.recordFailure(post.getId(), clientIp);
            throw new ValidateException(ErrorCode.POST_PASSWORD_MISMATCH);
        }

        passwordAttemptLimiter.clearFailures(post.getId(), clientIp);
    }

    /** 최대 세 개뿐이라 추가·삭제를 비교하지 않고 통째로 갈아끼운다 */
    private List<String> replaceTags(Post post, List<String> rawTags) {
        List<String> tags = TagNormalizer.normalize(rawTags);

        postTagRepository.deleteByPost(post);
        tags.forEach(tag -> postTagRepository.save(PostTag.builder()
                .post(post)
                .tag(tag)
                .build()));

        return tags;
    }

    private List<String> findTagsOf(Post post) {
        return postTagRepository.findByPostOrderByIdAsc(post).stream()
                .map(PostTag::getTag)
                .toList();
    }

    /** 목록용 일괄 조회. 글마다 따로 조회하면 20행 페이지에 쿼리가 21번 나간다 */
    private Map<Long, List<String>> findTagsOf(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        // id 순 조회 + groupingBy 가 순서를 유지하므로 첫 태그가 곧 대표 태그다
        return postTagRepository.findByPostIdInOrderByIdAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        postTag -> postTag.getPost().getId(),
                        Collectors.mapping(PostTag::getTag, Collectors.toList())));
    }

    private Post findActivePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.POST_NOT_FOUND));
    }

    private boolean isWrittenBy(Post post, ClientInfo clientInfo) {
        return clientInfo != null && post.isWrittenBy(clientInfo.getClientId());
    }

    private String resolveNickname(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return ANONYMOUS_NICKNAME;
        }
        return clientName;
    }
}
