package com.practicket.captcha.component;

import com.practicket.captcha.dto.CaptchaGlobalStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CaptchaGlobalStatCache {

    private static final int BIN_COUNT = 10;
    private static final double LOWER_RATIO = 0.05;
    private static final double UPPER_RATIO = 0.95;
    private static final int TOP_RANKING_LIMIT = 5;
    private static final Duration TTL = Duration.ofMinutes(1);

    private final CaptchaResultManager captchaResultManager;

    private volatile CaptchaGlobalStat snapshot;
    private volatile Instant expiresAt = Instant.EPOCH;

    /** 방금 기록을 낸 사람에게 "오늘 기록이 없다"고 보이지 않도록 저장 직후 비운다 */
    public void invalidate() {
        expiresAt = Instant.EPOCH;
    }

    public CaptchaGlobalStat get() {
        CaptchaGlobalStat cached = snapshot;
        if (cached != null && Instant.now().isBefore(expiresAt)) {
            return cached;
        }
        return refresh();
    }

    private synchronized CaptchaGlobalStat refresh() {
        if (snapshot != null && Instant.now().isBefore(expiresAt)) {
            return snapshot;
        }

        CaptchaGlobalStat computed = compute();
        snapshot = computed;
        expiresAt = Instant.now().plus(TTL);
        return computed;
    }

    private CaptchaGlobalStat compute() {
        LocalDateTime todayStart = todayStart();
        long totalCount = captchaResultManager.countAll();
        if (totalCount == 0) {
            return new CaptchaGlobalStat(0, 0f, 0f, 1f, new long[BIN_COUNT],
                    captchaResultManager.findTopRanking(todayStart, TOP_RANKING_LIMIT),
                    captchaResultManager.countPeopleSince(todayStart));
        }

        float lowerBound = roundToHalf(captchaResultManager.quantile(LOWER_RATIO));
        float upperBound = roundToHalf(captchaResultManager.quantile(UPPER_RATIO));
        float binWidth = roundToHalf((upperBound - lowerBound) / BIN_COUNT);
        if (binWidth <= 0) {
            binWidth = 0.5f;
        }

        return new CaptchaGlobalStat(
                totalCount,
                captchaResultManager.averageAll(),
                lowerBound,
                binWidth,
                captchaResultManager.histogram(lowerBound, binWidth, BIN_COUNT),
                captchaResultManager.findTopRanking(todayStart, TOP_RANKING_LIMIT),
                captchaResultManager.countPeopleSince(todayStart));
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    /** 기록이 쌓일 때마다 축 눈금이 미세하게 흔들리면 어제와 비교가 안 되므로 0.5초 단위로 고정한다 */
    private float roundToHalf(float value) {
        return Math.round(value * 2) / 2f;
    }
}
