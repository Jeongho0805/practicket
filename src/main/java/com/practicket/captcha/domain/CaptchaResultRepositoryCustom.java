package com.practicket.captcha.domain;

import com.practicket.captcha.dto.CaptchaMyStat;
import com.practicket.captcha.dto.CaptchaRankRow;

import java.time.LocalDateTime;
import java.util.List;

public interface CaptchaResultRepositoryCustom {

    long countAll();

    float averageAll();

    long countFasterThan(float elapsedSecond);

    float quantile(double ratio);

    long[] histogram(float lowerBound, float binWidth, int binCount);

    List<CaptchaRankRow> findTopRanking(LocalDateTime from, int limit);

    long countPeopleSince(LocalDateTime from);

    CaptchaMyStat findMyStat(Long clientId);

    List<Float> findRecentElapsedSeconds(Long clientId, int limit);
}
