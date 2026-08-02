package com.practicket.community.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/** 선택지를 고정해야 운영자마다 "대충 3일" 같은 감으로 값이 갈리지 않는다 */
@Getter
@RequiredArgsConstructor
public enum BanDuration {

    ONE_DAY("1일 (도배)", 1L),
    SEVEN_DAYS("7일 (반복)", 7L),
    PERMANENT("영구 (악질)", null);

    private final String label;
    private final Long days;

    /** null 이면 영구정지 */
    public LocalDateTime resolveUntil(LocalDateTime now) {
        return days == null ? null : now.plusDays(days);
    }
}
