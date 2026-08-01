package com.practicket.community.domain.entity;

/** 신고 대상. 글과 댓글을 한 테이블에서 받는다(docs/community-system.md Q7). */
public enum ReportTargetType {
    POST,
    COMMENT
}
