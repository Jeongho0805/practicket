package com.practicket.notice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 라벨을 여기서 들고 있어야 템플릿에 분기를 쌓지 않는다 */
@Getter
@RequiredArgsConstructor
public enum NoticeType {

    NOTICE("공지", "t-notice"),
    FIX("수정", "t-fix");

    private final String label;
    /** landing.css 의 배지 클래스명 */
    private final String badgeClass;
}
