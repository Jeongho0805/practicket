package com.practicket.art.dto;

import com.practicket.art.domain.entity.Art;
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
    private Integer commentCount;
    private String authorName;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isLikedByCurrentUser;
    private Boolean isOwnedByCurrentUser;

    public static ArtResponse from(Art art) {
        return ArtResponse.builder()
                .id(art.getId())
                .title(art.getTitle())
                .pixelData(art.getPixelData())
                .width(art.getWidth())
                .height(art.getHeight())
                .likeCount(art.getLikeCount())
                .viewCount(art.getViewCount())
                .commentCount(art.getCommentCount())
                .authorName(art.getClient().getName() != null ? art.getClient().getName() : "익명")
                .authorId(art.getClient().getId())
                .createdAt(art.getCreatedAt())
                .updatedAt(art.getUpdatedAt())
                .isLikedByCurrentUser(false)
                .isOwnedByCurrentUser(false)
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
                .commentCount(art.getCommentCount())
                .authorName(art.getClient().getName() != null ? art.getClient().getName() : "익명")
                .authorId(art.getClient().getId())
                .createdAt(art.getCreatedAt())
                .updatedAt(art.getUpdatedAt())
                .isLikedByCurrentUser(isLiked)
                .isOwnedByCurrentUser(false)
                .build();
    }

    public static ArtResponse from(Art art, boolean isLiked, boolean isOwned) {
        return ArtResponse.builder()
                .id(art.getId())
                .title(art.getTitle())
                .pixelData(art.getPixelData())
                .width(art.getWidth())
                .height(art.getHeight())
                .likeCount(art.getLikeCount())
                .viewCount(art.getViewCount())
                .commentCount(art.getCommentCount())
                .authorName(art.getClient().getName() != null ? art.getClient().getName() : "익명")
                .authorId(art.getClient().getId())
                .createdAt(art.getCreatedAt())
                .updatedAt(art.getUpdatedAt())
                .isLikedByCurrentUser(isLiked)
                .isOwnedByCurrentUser(isOwned)
                .build();
    }
}