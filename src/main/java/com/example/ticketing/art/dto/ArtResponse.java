package com.example.ticketing.art.dto;

import com.example.ticketing.art.domain.Art;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArtResponse {

    private Long id;
    private String title;
    private String pixelData;
    private Integer width;
    private Integer height;
    private Integer likeCount;
    private Integer viewCount;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isLikedByCurrentUser;

    public static ArtResponse from(Art art) {
        return ArtResponse.builder()
                .id(art.getId())
                .title(art.getTitle())
                .pixelData(art.getPixelData())
                .width(art.getWidth())
                .height(art.getHeight())
                .likeCount(art.getLikeCount())
                .viewCount(art.getViewCount())
                .authorName(art.getClient().getName() != null ? art.getClient().getName() : "익명")
                .createdAt(art.getCreatedAt())
                .updatedAt(art.getUpdatedAt())
                .isLikedByCurrentUser(false)
                .build();
    }

    public static ArtResponse from(Art art, boolean isLiked) {
        return ArtResponse.builder()
                .id(art.getId())
                .title(art.getTitle())
                .pixelData(art.getPixelData())
                .width(art.getWidth())
                .height(art.getHeight())
                .likeCount(art.getLikeCount())
                .viewCount(art.getViewCount())
                .authorName(art.getClient().getName() != null ? art.getClient().getName() : "익명")
                .createdAt(art.getCreatedAt())
                .updatedAt(art.getUpdatedAt())
                .isLikedByCurrentUser(isLiked)
                .build();
    }
}