package com.practicket.community.dto;

import com.practicket.community.component.IpMasker;
import com.practicket.community.domain.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 목록 한 행. 본문은 담지 않는다 */
@Getter
@Builder
public class PostListResponse {

    /** N 배지 기준. 오픈 후 실제 발행 속도를 보고 다시 조정한다 */
    private static final Duration NEW_WINDOW = Duration.ofHours(6);

    /** 화면에서만 가리면 API 를 직접 불러 원문을 읽을 수 있다. 여기서도 막는다 */
    private static final String BLINDED_TITLE = "숨겨진 글";

    private final Long id;
    private final String title;
    private final String nickname;
    private final String ip;
    private final Long likeCount;
    private final Long commentCount;
    private final Long viewCount;
    private final Boolean blinded;
    private final LocalDateTime createdAt;

    /** 목록에는 대표 태그만 보인다 */
    private final List<String> tags;

    /** 화면의 N 배지 */
    private final Boolean isNew;

    public static PostListResponse from(Post post) {
        return from(post, List.of());
    }

    public static PostListResponse from(Post post, List<String> tags) {
        boolean blinded = Boolean.TRUE.equals(post.getBlinded());

        return PostListResponse.builder()
                .id(post.getId())
                .title(blinded ? BLINDED_TITLE : post.getTitle())
                .nickname(post.getNickname())
                .ip(IpMasker.maskToTwoSegments(post.getIp()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .blinded(post.getBlinded())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .isNew(isNew(post.getCreatedAt()))
                .build();
    }

    private static boolean isNew(LocalDateTime createdAt) {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minus(NEW_WINDOW));
    }
}
