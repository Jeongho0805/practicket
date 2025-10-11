package com.example.ticketing.art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtLikeResponse {
    private Boolean isLiked;
    private int likeCount;
}
