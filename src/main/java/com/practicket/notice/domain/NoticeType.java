package com.practicket.notice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공지 종류. 화면 배지 문구·색이 여기서 갈린다.
 * 라벨을 엔티티가 들고 있어야 템플릿에서 분기(th:if)를 쌓지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum NoticeType {

    NOTICE("공지", "t-notice"),
    FIX("수정", "t-fix");

    private final String label;
    /** landing.css 의 배지 클래스명. 홈과 목록이 같은 스타일을 쓴다. */
    private final String badgeClass;
}
