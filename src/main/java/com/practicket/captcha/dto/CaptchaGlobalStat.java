package com.practicket.captcha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CaptchaGlobalStat {

    private long totalCount;

    private float totalAverage;

    private float lowerBound;

    private float binWidth;

    private long[] counts;

    private List<CaptchaRankRow> topRanking;

    private long todayPeople;
}
