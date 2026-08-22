package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.component.ProfanityValidator;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.PostCommentRateLimiter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.PostCommentCreateRequest;
import com.practicket.community.dto.PostCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 삭제는 토큰이 작성자일 때만. 글과 달리 비밀번호를 받지 않는다 — 댓글마다 입력하면 마찰이 크다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    /** 닉네임을 설정하지 않은 사용자의 표시 이름 */
    private static final String ANONYMOUS_NICKNAME = "익명";

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final ClientManager clientManager;
    private final PostCommentRateLimiter commentRateLimiter;
    private final ProfanityValidator profanityValidator;

    public List<PostCommentResponse> list(Long postId, ClientInfo clientInfo) {
        Post post = findActivePost(postId);
        return postCommentRepository.findByPostOrderByCreatedAtAsc(post).stream()
                .map(comment -> PostCommentResponse.from(comment, isWrittenBy(comment, clientInfo)))
                .toList();
    }

    @Transactional
    public PostCommentResponse create(Long postId, PostCommentCreateRequest request,
                                      ClientInfo clientInfo, String clientIp) {
        Post post = findActivePost(postId);
        Client client = clientManager.findById(clientInfo.getClientId());

        // 이미 막힌 사용자를 레이트리밋·욕설 필터까지 태워볼 이유가 없다
        if (client.isBanned(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.COMMENT_BANNED);
        }

        // fail-open — 외부 API 장애로 댓글이 막히는 게 더 나쁘다
        profanityValidator.validateProfanityText(request.getContent());

        // 검증을 통과한 요청만 센다. 거부된 댓글이 할당량을 먹으면 안 된다
        commentRateLimiter.validate(clientInfo.getToken(), clientIp);

        PostComment comment = postCommentRepository.save(PostComment.builder()
                .post(post)
                .client(client)
                .content(request.getContent())
                .nickname(resolveNickname(client.getName()))
                .ip(clientIp)
                .build());

        // 목록 배지가 이 컬럼을 읽는다. 매번 COUNT 하지 않는다
        postRepository.incrementCommentCount(postId);

        return PostCommentResponse.from(comment, true);
    }

    /** 행을 지우지 않고 deletedAt 만 남긴다 — 신고·모더레이션이 원문을 봐야 한다 */
    @Transactional
    public void delete(Long commentId, ClientInfo clientInfo) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!isWrittenBy(comment, clientInfo)) {
            throw new ValidateException(ErrorCode.COMMENT_FORBIDDEN);
        }

        comment.softDelete(LocalDateTime.now());
        postRepository.decrementCommentCount(comment.getPost().getId());
    }

    private Post findActivePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.POST_NOT_FOUND));
    }

    private boolean isWrittenBy(PostComment comment, ClientInfo clientInfo) {
        return clientInfo != null && comment.isWrittenBy(clientInfo.getClientId());
    }

    private String resolveNickname(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return ANONYMOUS_NICKNAME;
        }
        return clientName;
    }
}
