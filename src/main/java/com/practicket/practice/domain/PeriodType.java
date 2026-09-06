package com.practicket.practice.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME;

    /**
     * 전체 기간에는 시작일이 없지만 NULL 로 둘 수 없다. 유니크 제약이 NULL 끼리를 서로 다른 값으로
     * 보기 때문에 사람마다 전체 기간 줄이 여러 개 생겨도 막지 못한다.
     */
    public static final LocalDate ALL_TIME_START = LocalDate.of(1970, 1, 1);

    /** 그 날짜가 속한 기간의 첫날. practice_best_result 의 period_start 값이 된다. */
    public LocalDate bucketStart(LocalDate date) {
        return switch (this) {
            case DAILY -> date;
            case WEEKLY -> date.with(DayOfWeek.MONDAY);
            case MONTHLY -> date.withDayOfMonth(1);
            case ALL_TIME -> ALL_TIME_START;
        };
    }

    public LocalDate currentBucketStart() {
        return bucketStart(LocalDate.now());
    }
}
