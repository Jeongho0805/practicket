package com.practicket.community.application;

import com.practicket.client.component.ClientInfoExtractor;
import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.community.dto.PostCommentCreateRequest;
import com.practicket.community.dto.PostCommentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService postCommentService;
    private final ClientInfoExtractor clientInfoExtractor;

    /** 어느 댓글이 내 것인지는 토큰을 아는 브라우저만 판단할 수 있어 다시 받아온다 */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<PostCommentResponse>> listComments(
            @PathVariable Long postId,
            @Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(postCommentService.list(postId, clientInfo));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<PostCommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody PostCommentCreateRequest request,
            @Auth ClientInfo clientInfo,
            HttpServletRequest httpRequest) {
        String clientIp = clientInfoExtractor.extractClientInfo(httpRequest).getIp();
        return ResponseEntity.ok(postCommentService.create(postId, request, clientInfo, clientIp));
    }

    /** 비밀번호가 없다. 토큰이 작성자와 같을 때만 지워진다 */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @Auth ClientInfo clientInfo) {
        postCommentService.delete(commentId, clientInfo);
        return ResponseEntity.noContent().build();
    }
}
