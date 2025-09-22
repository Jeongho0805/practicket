package com.example.ticketing.captcha.dto;

import lombok.*;

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

    private float avgPercentile;

    private float bestResult;
}
