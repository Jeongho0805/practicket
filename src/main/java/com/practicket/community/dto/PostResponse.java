package com.practicket.community.dto;

import com.practicket.community.component.IpMasker;
import com.practicket.community.domain.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 글 상세.
 *
 * 작성자 표시는 글 행에 박아둔 스냅샷을 그대로 쓴다(Client 조인 없음).
 * IP 는 앞 두 마디만 내보낸다 — 전체 IP 는 서버 밖으로 나가지 않는다.
 */
@Getter
@Builder
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;
    private final String ip;
    private final Long likeCount;
    private final Long commentCount;
    private final Long viewCount;
    private final Boolean edited;
    private final Boolean blinded;
    private final LocalDateTime createdAt;

    /** 이 요청을 보낸 토큰이 작성자인지. 화면의 수정·삭제 버튼 노출 기준. */
    private final Boolean mine;

    public static PostResponse from(Post post, boolean mine) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .nickname(post.getNickname())
                .ip(IpMasker.maskToTwoSegments(post.getIp()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .edited(post.getEdited())
                .blinded(post.getBlinded())
                .createdAt(post.getCreatedAt())
                .mine(mine)
                .build();
    }
}
