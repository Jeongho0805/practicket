package com.practicket.practice.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY;

    /** 그 날짜가 속한 기간의 첫날. practice_best_result 의 period_start 값이 된다. */
    public LocalDate bucketStart(LocalDate date) {
        return switch (this) {
            case DAILY -> date;
            case WEEKLY -> date.with(DayOfWeek.MONDAY);
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    public LocalDate currentBucketStart() {
        return bucketStart(LocalDate.now());
    }
}
