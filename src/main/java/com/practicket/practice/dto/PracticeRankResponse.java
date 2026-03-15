package com.practicket.practice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PracticeRankResponse {

    private final List<PracticeRankItem> data;
    private final NextCursor nextCursor;
    private final boolean hasNext;

    @Getter
    @RequiredArgsConstructor
    public static class NextCursor {
        private final int totalDurationMs;
        private final long id;
    }
}
