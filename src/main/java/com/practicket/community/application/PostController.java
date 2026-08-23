package com.practicket.community.application;

import com.practicket.client.component.ClientInfoExtractor;
import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.community.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ClientInfoExtractor clientInfoExtractor;

    /** 무한스크롤이 아니라 페이지 번호다 — 크롤러가 2페이지 이후를 못 본다 */
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> searchPosts(
            @ModelAttribute PostSearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.search(condition, pageable));
    }

    /** 경로가 {@code /{postId}} 보다 먼저 와야 한다 — 나중에 두면 "mine" 을 글 번호로 읽는다 */
    @GetMapping("/mine")
    public ResponseEntity<Page<PostListResponse>> searchMyPosts(
            @Auth ClientInfo clientInfo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.searchMine(clientInfo, pageable));
    }

    /** 서버 렌더링 뒤 브라우저가 부른다. 조회수도 여기서 오른다 */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @Auth ClientInfo clientInfo,
            @PathVariable Long postId) {
        return ResponseEntity.ok(postService.getAndCountView(postId, clientInfo));
    }

    /** 이미 눌렀으면 취소된다. 본인 글은 거부 */
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> toggleLike(
            @PathVariable Long postId,
            @Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(postService.toggleLike(postId, clientInfo));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @Auth ClientInfo clientInfo,
            HttpServletRequest httpRequest) {
        String clientIp = clientInfoExtractor.extractClientInfo(httpRequest).getIp();
        return ResponseEntity.ok(postService.create(request, clientInfo, clientIp));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(postService.update(postId, request, clientInfo));
    }

    /** 비밀번호를 본문으로 받는 이유: 쿼리 파라미터에 실으면 접근 로그에 남는다 */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @RequestBody(required = false) PostDeleteRequest request,
            @Auth ClientInfo clientInfo,
            HttpServletRequest httpRequest) {
        String clientIp = clientInfoExtractor.extractClientInfo(httpRequest).getIp();
        postService.delete(postId, request, clientInfo, clientIp);
        return ResponseEntity.noContent().build();
    }
}
