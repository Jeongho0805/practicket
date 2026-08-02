package com.practicket.captcha.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaptchaResultStatistic {

    private int latestRank;

    private float latestResult;

    private float myAvgResult;

    private float totalAvgResult;

    private float latestPercentile;

    private float bestResult;

    private long myCount;

    private long totalCount;

    private CaptchaDistribution distribution;

    private List<Float> recentResults;

    private List<CaptchaRankRow> topRanking;

    private long todayPeople;
}
