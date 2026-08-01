package com.practicket.notice.dto;

import com.practicket.notice.domain.Notice;

import java.time.format.DateTimeFormatter;

/** 배지 라벨·날짜 문자열을 서버가 만들어 내린다 — 화면이 enum 을 다시 해석하지 않게 */
public record NoticeListResponse(
        Long id,
        String typeLabel,
        String badgeClass,
        boolean isNotice,
        boolean pinned,
        String title,
        String summary,
        String date,
        String monthKey,
        String monthLabel
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    public static NoticeListResponse from(Notice notice) {
        String monthKey = notice.getCreatedAt().format(MONTH_KEY);
        String[] ym = monthKey.split("-");
        return new NoticeListResponse(
                notice.getId(),
                notice.getType().getLabel(),
                notice.getType().getBadgeClass(),
                notice.getType().name().equals("NOTICE"),
                Boolean.TRUE.equals(notice.getPinned()),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt().format(DATE),
                monthKey,
                ym[0] + "년 " + Integer.parseInt(ym[1]) + "월"
        );
    }
}
