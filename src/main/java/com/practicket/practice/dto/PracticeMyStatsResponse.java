package com.practicket.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeMyStatsResponse {

    private final Integer bestMs;    // null = 전체 기록 없음
    private final int totalCount;
}
