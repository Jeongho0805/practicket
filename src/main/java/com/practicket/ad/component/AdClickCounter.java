package com.practicket.ad.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 배너 클릭 카운트. Redis INCR로 쌓고 AdStatFlushScheduler가 주기적으로 DB에 반영한다.
 * 봇/오클릭 필터는 최소한만: 동일 IP가 짧은 시간 내 같은 배너를 재클릭하면 무시한다.
 */
@Component
@RequiredArgsConstructor
public class AdClickCounter {

    private static final String KEY_PREFIX = "ad:clk:";
    // 주의: "ad:clk:"로 시작하면 안 됨 (AdStatFlushScheduler가 ad:clk:* 패턴으로 카운터 키를 스캔함)
    private static final String DEDUP_KEY_PREFIX = "ad:clkdedup:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long DEDUP_WINDOW_SECONDS = 5L;
    private static final long SAFETY_TTL_DAYS = 3L;

    private final StringRedisTemplate stringRedisTemplate;

    public void record(Long bannerId, String clientIp) {
        if (bannerId == null) {
            return;
        }
        if (!isFirstClickInWindow(bannerId, clientIp)) {
            return;
        }
        String key = buildKey(bannerId, LocalDate.now());
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, SAFETY_TTL_DAYS, TimeUnit.DAYS);
        }
    }

    private boolean isFirstClickInWindow(Long bannerId, String clientIp) {
        String ip = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        String dedupKey = DEDUP_KEY_PREFIX + bannerId + ":" + ip;
        Boolean firstSeen = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", DEDUP_WINDOW_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(firstSeen);
    }

    public static String buildKey(Long bannerId, LocalDate date) {
        return KEY_PREFIX + bannerId + ":" + date.format(DATE_FORMATTER);
    }

    public static String keyPattern() {
        return KEY_PREFIX + "*";
    }

    public static String keyPrefix() {
        return KEY_PREFIX;
    }
}
