package com.practicket.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 랭킹 목록 하단에 고정으로 붙는 내 줄. 선택된 기간 기준이라 위 목록과 순위 기준이 같다. */
@Getter
@AllArgsConstructor
public class PracticeMyRankResponse {

    /** null = 해당 기간에 기록 없음 */
    private final Long rank;
    private final String nickname;
    private final Integer bestMs;
    private final Integer reactionTimeMs;
    private final Integer queueWaitMs;
    private final Integer captchaMs;
    private final Integer seatSelectionMs;
    private final long totalUsers;
}
