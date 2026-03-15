package com.practicket.practice.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public enum PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY;

    public LocalDateTime getStartDateTime() {
        return switch (this) {
            case DAILY -> LocalDate.now().atStartOfDay();
            case WEEKLY -> LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
            case MONTHLY -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
        };
    }
}
