package com.example.ticketing.art.application;

import com.example.ticketing.art.dto.ArtCreateRequest;
import com.example.ticketing.art.dto.ArtResponse;
import com.example.ticketing.art.dto.ArtUpdateRequest;
import com.example.ticketing.common.auth.Auth;
import com.example.ticketing.common.auth.ClientInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Page<ArtResponse>> getPublicArts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ArtResponse> response = artService.getPublicArts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{artId}")
    public ResponseEntity<ArtResponse> getArt(@PathVariable Long artId) {
        ArtResponse response = artService.getArt(artId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ArtResponse>> getMyArts(
            @Auth ClientInfo clientInfo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ArtResponse> response = artService.getMyArts(clientInfo, pageable);
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
}