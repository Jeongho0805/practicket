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

    /** 목록. 무한스크롤이 아니라 페이지 번호다 — 크롤러가 2페이지 이후를 못 본다(Q9). */
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> searchPosts(
            @ModelAttribute PostSearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.search(condition, pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @Auth ClientInfo clientInfo,
            @PathVariable Long postId) {
        return ResponseEntity.ok(postService.get(postId, clientInfo));
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

    /**
     * 비밀번호는 본문으로 받는다. 쿼리 파라미터에 실으면 접근 로그에 그대로 남는다.
     * 토큰이 작성자와 같으면 본문 없이 호출해도 된다.
     */
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
