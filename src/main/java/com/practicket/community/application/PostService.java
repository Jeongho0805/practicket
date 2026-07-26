package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.DeletePasswordEncoder;
import com.practicket.community.component.PostPasswordAttemptLimiter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.repository.PostQueryCondition;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    /** 닉네임을 설정하지 않은 사용자의 표시 이름. 닉네임 게이트를 세우지 않는다(Q1). */
    private static final String ANONYMOUS_NICKNAME = "익명";

    private final PostRepository postRepository;
    private final ClientManager clientManager;
    private final DeletePasswordEncoder deletePasswordEncoder;
    private final PostPasswordAttemptLimiter passwordAttemptLimiter;

    @Transactional
    public PostResponse create(PostCreateRequest request, ClientInfo clientInfo, String clientIp) {
        Client client = clientManager.findById(clientInfo.getClientId());

        Post post = Post.builder()
                .client(client)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .nickname(resolveNickname(client.getName()))
                .ip(clientIp)
                .deletePasswordHash(deletePasswordEncoder.encode(request.getDeletePassword()))
                .build();

        return PostResponse.from(postRepository.save(post), true);
    }

    public Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable) {
        PostQueryCondition queryCondition = PostQueryCondition.builder()
                .keyword(condition.getKeyword())
                .build();

        return postRepository.search(queryCondition, pageable)
                .map(PostListResponse::from);
    }

    public PostResponse get(Long postId, ClientInfo clientInfo) {
        Post post = findActivePost(postId);
        return PostResponse.from(post, isWrittenBy(post, clientInfo));
    }

    /**
     * 수정은 토큰이 작성자와 같을 때만 된다.
     * 삭제 비밀번호로는 수정할 수 없다 — 비밀번호는 삭제 전용 비상 열쇠다(Q5).
     */
    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest request, ClientInfo clientInfo) {
        Post post = findActivePost(postId);

        if (!isWrittenBy(post, clientInfo)) {
            throw new ValidateException(ErrorCode.POST_FORBIDDEN);
        }

        post.update(request.getTitle().trim(), request.getContent());
        return PostResponse.from(post, true);
    }

    /**
     * 토큰이 작성자면 비밀번호를 묻지 않고, 다르면 네 자리 비밀번호를 대조한다.
     * 행을 지우지 않고 deletedAt 만 남긴다.
     */
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

        passwordAttemptLimiter.validateNotLocked(clientIp);

        if (!deletePasswordEncoder.matches(rawPassword, post.getDeletePasswordHash())) {
            passwordAttemptLimiter.recordFailure(clientIp);
            throw new ValidateException(ErrorCode.POST_PASSWORD_MISMATCH);
        }

        passwordAttemptLimiter.clearFailures(clientIp);
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
