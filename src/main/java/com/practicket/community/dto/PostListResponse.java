package com.practicket.community.dto;

import com.practicket.community.component.IpMasker;
import com.practicket.community.domain.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 목록 한 행. 본문은 담지 않는다 — 목록에서 TEXT 를 전부 실어 나를 이유가 없다.
 */
@Getter
@Builder
public class PostListResponse {

    private final Long id;
    private final String title;
    private final String nickname;
    private final String ip;
    private final Long likeCount;
    private final Long commentCount;
    private final Long viewCount;
    private final Boolean blinded;
    private final LocalDateTime createdAt;

    public static PostListResponse from(Post post) {
        return PostListResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .nickname(post.getNickname())
                .ip(IpMasker.maskToTwoSegments(post.getIp()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .blinded(post.getBlinded())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
