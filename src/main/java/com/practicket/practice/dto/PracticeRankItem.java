package com.practicket.practice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PracticeRankItem {

    private final String nickname;
    private final int totalDurationMs;
    private final int reactionTimeMs;
    private final int queueWaitMs;
    private final int seatSelectionMs;
}
