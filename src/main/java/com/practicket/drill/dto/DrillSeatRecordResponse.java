package com.practicket.drill.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** 포도알·취소표 기록 카드. missed 는 취소표 일반형만 값이 있다. */
public record DrillSeatRecordResponse(long count, Item best, CleanBest cleanBest, List<Item> recent) {

    public static DrillSeatRecordResponse of(long count, Item best, Optional<Item> cleanBest, List<Item> latestFirst) {
        List<Item> recent = new ArrayList<>(latestFirst);
        Collections.reverse(recent);
        return new DrillSeatRecordResponse(count, best,
                cleanBest.map(c -> new CleanBest(c.ms(), c.createdAt())).orElse(null), recent);
    }

    public static DrillSeatRecordResponse empty() {
        return new DrillSeatRecordResponse(0, null, null, List.of());
    }

    public record Item(int ms, int misclicks, Integer missed, LocalDateTime createdAt) {
    }

    public record CleanBest(int ms, LocalDateTime createdAt) {
    }
}
