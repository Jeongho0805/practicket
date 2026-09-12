package com.practicket.practice.dto;

import java.util.List;

/**
 * 기록 분포와 등급 컷. 화면은 이 하나로 그래프와 등급을 모두 그린다.
 * <p>
 * bins 는 85% 컷까지만 담는다. 그보다 느린 사람은 그래프에 안 그린다 —
 * 마지막 칸에 몰아넣으면 그 칸 하나가 봉우리를 덮는다. 그 인원은 totalUsers 에서 bins 합을 빼면 나온다.
 */
public record PracticeDistributionResponse(
        int binStartMs,
        int binWidthMs,
        List<Integer> bins,
        List<Double> tierPercentiles,
        List<Integer> tierCutMs,
        long totalUsers
) {

    public static PracticeDistributionResponse empty(List<Double> tierPercentiles) {
        return new PracticeDistributionResponse(0, 0, List.of(), tierPercentiles, List.of(), 0);
    }
}
