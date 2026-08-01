package com.practicket.community.admin.dto;

import java.time.LocalDateTime;

/** 프로젝션이라 Post 의 {@code @SQLRestriction} 을 타지 않는다 — 삭제된 글까지 보인다 */
public interface AdminPostView {

    Long getId();

    Long getClientId();

    String getTitle();

    String getContent();

    String getNickname();

    String getIp();

    Long getViewCount();

    Long getLikeCount();

    Long getCommentCount();

    Integer getReportCount();

    Boolean getBlinded();

    Boolean getEdited();

    LocalDateTime getCreatedAt();

    /** null 이면 살아있는 글 */
    LocalDateTime getDeletedAt();
}
