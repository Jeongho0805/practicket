package com.practicket.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PracticeMyRecordsResponse {

    private final List<RecordItem> data;
    private final Long nextCursor;  // null = 더 없음 (마지막 항목의 id)
    private final boolean hasNext;

    @Getter
    @AllArgsConstructor
    public static class RecordItem {
        private final int totalDurationMs;
        private final int reactionTimeMs;
        private final int queueWaitMs;
        private final int seatSelectionMs;
        private final int queueInitialRank;
        private final LocalDateTime startedAt;
    }
}
