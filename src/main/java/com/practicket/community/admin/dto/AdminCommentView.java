package com.practicket.community.admin.dto;

import java.time.LocalDateTime;

/** {@link AdminPostView} 와 같은 이유로 엔티티 대신 프로젝션을 쓴다 */
public interface AdminCommentView {

    Long getId();

    Long getPostId();

    Long getClientId();

    String getContent();

    String getNickname();

    String getIp();

    Integer getReportCount();

    Boolean getBlinded();

    LocalDateTime getCreatedAt();

    LocalDateTime getDeletedAt();
}
