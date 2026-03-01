package com.practicket.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeMyStatsResponse {

    private final Long monthlyRank;  // null = 이번 달 기록 없음
    private final Integer bestMs;    // null = 전체 기록 없음
    private final Integer firstMs;   // 첫 기록 totalDurationMs (개선도 계산용)
    private final int totalCount;
}
