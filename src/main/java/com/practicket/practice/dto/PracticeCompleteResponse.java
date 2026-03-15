package com.practicket.practice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PracticeCompleteResponse {

    private final int totalDurationMs;
    private final int reactionTimeMs;
    private final int queueWaitMs;
    private final int seatSelectionMs;
    private final int queueInitialRank;

    /** 이번 달 상위 몇 % (1~100). 데이터 없으면 null */
    private final Integer percentile;

    /** 이번 달 내 순위 */
    private final Integer myRank;

    /** 이번 달 참여 유저 수 */
    private final Integer totalUsers;
}
