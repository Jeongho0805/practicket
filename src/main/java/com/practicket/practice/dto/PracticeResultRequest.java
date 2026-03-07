package com.practicket.practice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PracticeResultRequest {

    @NotBlank
    private String sessionId;

    @Min(0)
    private int totalDurationMs;

    @Min(0)
    private int reactionTimeMs;

    @Min(0)
    private int queueWaitMs;

    @Min(0)
    private int seatSelectionMs;

    @Min(0)
    private int queueInitialRank;
}
