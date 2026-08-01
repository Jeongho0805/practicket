package com.practicket.community.dto;

import com.practicket.community.component.IpMasker;
import com.practicket.community.component.PostContentRenderer;
import com.practicket.community.domain.entity.PostComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 본문은 이미 그려진 HTML 로만 내보낸다. 원문을 같이 실으면 innerHTML 에 꽂는 코드가 생긴다 */
@Getter
@Builder
public class PostCommentResponse {

    private final Long id;
    private final String renderedContent;
    private final String nickname;
    private final String ip;
    private final Boolean blinded;

    /** 아바타 색 번호(1~5). 닉네임에서 뽑는다 */
    private final int avatarColor;
    private final LocalDateTime createdAt;

    /** 삭제 버튼 노출 기준 */
    private final Boolean mine;

    private static final int AVATAR_COLORS = 5;

    /** 완전히 지우지 않고 자리를 남겨야 신고 어뷰징이 드러난다 */
    private static final String BLINDED_NOTICE = "신고 누적으로 숨겨진 댓글입니다";

    public static PostCommentResponse from(PostComment comment, boolean mine) {
        return PostCommentResponse.builder()
                .id(comment.getId())
                .renderedContent(resolveRenderedContent(comment))
                .nickname(comment.getNickname())
                .ip(IpMasker.maskToTwoSegments(comment.getIp()))
                .blinded(comment.getBlinded())
                .avatarColor(avatarColorOf(comment.getNickname()))
                .createdAt(comment.getCreatedAt())
                .mine(mine)
                .build();
    }

    /** JS 가 이 DTO 를 그대로 다시 그리므로(refreshComments) 가리는 일은 여기서 해야 한다 */
    private static String resolveRenderedContent(PostComment comment) {
        if (Boolean.TRUE.equals(comment.getBlinded())) {
            return BLINDED_NOTICE;
        }
        return PostContentRenderer.renderComment(comment.getContent());
    }

    /** 같은 닉네임이면 항상 같은 색 */
    private static int avatarColorOf(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return 1;
        }
        return Math.floorMod(nickname.hashCode(), AVATAR_COLORS) + 1;
    }
}
