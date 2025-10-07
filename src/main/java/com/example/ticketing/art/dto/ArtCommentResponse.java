package com.example.ticketing.art.dto;

import com.example.ticketing.art.domain.entity.ArtComment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArtCommentResponse {

    private Long id;
    private String content;
    private String authorName;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArtCommentResponse from(ArtComment comment) {
        return ArtCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getClient().getName() != null ? comment.getClient().getName() : "익명")
                .authorId(comment.getClient().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
