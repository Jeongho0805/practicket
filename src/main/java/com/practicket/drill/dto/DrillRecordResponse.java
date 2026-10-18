package com.practicket.drill.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DrillRecordResponse<T>(long count, Integer bestMs, LocalDateTime bestAt, List<T> recent) {

    public static <T> DrillRecordResponse<T> empty() {
        return new DrillRecordResponse<>(0, null, null, List.of());
    }

    public record ReactItem(int ms, Integer latencyMs, LocalDateTime createdAt) {
    }

    public record CaptchaItem(int ms, LocalDateTime createdAt) {
    }
}
