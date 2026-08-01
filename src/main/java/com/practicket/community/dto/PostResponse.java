package com.practicket.community.dto;

import com.practicket.community.component.IpMasker;
import com.practicket.community.domain.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** IP 는 앞 두 마디만 내보낸다 — 전체 IP 는 서버 밖으로 나가지 않는다 */
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

    /** 0~3개. 안 단 글은 빈 목록이다 */
    private final List<String> tags;

    /** 수정·삭제 버튼 노출 기준 */
    private final Boolean mine;

    /** 서버 렌더링 시점에는 알 수 없어 항상 false 다 */
    private final Boolean liked;

    public static PostResponse from(Post post, boolean mine) {
        return from(post, mine, false, List.of());
    }

    public static PostResponse from(Post post, boolean mine, List<String> tags) {
        return from(post, mine, false, tags);
    }

    /**
     * 블라인드 글은 안내 문구가 나가되 작성자 본인에게는 원문을 준다 —
     * 수정 화면이 이 값으로 입력칸을 채워서, 가리면 고치는 순간 원문이 덮어써진다.
     */
    private static final String BLINDED_TITLE = "숨겨진 글";
    private static final String BLINDED_CONTENT = "신고 누적으로 숨겨진 글입니다.";

    public static PostResponse from(Post post, boolean mine, boolean liked, List<String> tags) {
        boolean hideContent = Boolean.TRUE.equals(post.getBlinded()) && !mine;

        return PostResponse.builder()
                .id(post.getId())
                // 광고글은 제목 자체가 광고라 남겨두면 <title> 과 검색 결과에 박힌다
                .title(hideContent ? BLINDED_TITLE : post.getTitle())
                .content(hideContent ? BLINDED_CONTENT : post.getContent())
                .nickname(post.getNickname())
                .ip(IpMasker.maskToTwoSegments(post.getIp()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .edited(post.getEdited())
                .blinded(post.getBlinded())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .mine(mine)
                .liked(liked)
                .build();
    }
}
