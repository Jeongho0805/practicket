package com.example.ticketing.art.application;

import com.example.ticketing.art.dto.*;
import com.example.ticketing.common.auth.Auth;
import com.example.ticketing.common.auth.ClientInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/arts")
@RequiredArgsConstructor
public class ArtController {

    private final ArtService artService;

    @GetMapping
    public ResponseEntity<Page<ArtResponse>> searchArts(
            @Auth ClientInfo clientInfo,
            @ModelAttribute ArtSearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ArtResponse> response = artService.searchArts(condition, clientInfo, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{artId}")
    public ResponseEntity<ArtResponse> getArt(@PathVariable Long artId) {
        ArtResponse response = artService.getArt(artId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ArtResponse> createArt(
            @Valid @RequestBody ArtCreateRequest request,
            @Auth ClientInfo clientInfo) {
        log.info("Request 정보 = {}", request);
        ArtResponse response = artService.createArt(request, clientInfo);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{artId}")
    public ResponseEntity<ArtResponse> updateArt(
            @PathVariable Long artId,
            @Valid @RequestBody ArtUpdateRequest request,
            @Auth ClientInfo clientInfo) {
        ArtResponse response = artService.updateArt(artId, request, clientInfo);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{artId}")
    public ResponseEntity<Void> deleteArt(
            @PathVariable Long artId,
            @Auth ClientInfo clientInfo) {
        artService.deleteArt(artId, clientInfo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{artId}/like")
    public ResponseEntity<ArtLikeResponse> toggleLike(@PathVariable Long artId, @Auth ClientInfo clientInfo) {
        ArtLikeResponse artLikeResponse = artService.toggleLike(artId, clientInfo);
        return ResponseEntity.ok(artLikeResponse);
    }

    @GetMapping("/{artId}/comments")
    public ResponseEntity<Page<ArtCommentResponse>> getComments(
            @PathVariable Long artId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ArtCommentResponse> comments = artService.getComments(artId, pageable);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{artId}/comments")
    public ResponseEntity<ArtCommentResponse> createComment(
            @PathVariable Long artId,
            @Valid @RequestBody ArtCommentRequest request,
            @Auth ClientInfo clientInfo) {
        ArtCommentResponse response = artService.createComment(artId, request, clientInfo);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ArtCommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody ArtCommentRequest request,
            @Auth ClientInfo clientInfo) {
        ArtCommentResponse response = artService.updateComment(commentId, request, clientInfo);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @Auth ClientInfo clientInfo) {
        artService.deleteComment(commentId, clientInfo);
        return ResponseEntity.noContent().build();
    }
}