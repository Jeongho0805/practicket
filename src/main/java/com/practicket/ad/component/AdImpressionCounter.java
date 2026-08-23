package com.practicket.ad.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 배너 노출 카운트. 매 렌더마다 DB에 쓰지 않고 Redis INCR만 한다.
 * AdStatFlushScheduler가 주기적으로 읽어 banner_stat_daily에 반영 후 정리한다.
 */
@Component
@RequiredArgsConstructor
public class AdImpressionCounter {

    private static final String KEY_PREFIX = "ad:imp:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    // 스케줄러가 flush에 실패해도 키가 영영 안 지워지지 않도록 넉넉한 안전망 TTL
    private static final long SAFETY_TTL_DAYS = 3L;

    private final StringRedisTemplate stringRedisTemplate;

    public void record(Long bannerId) {
        if (bannerId == null) {
            return;
        }
        String key = buildKey(bannerId, LocalDate.now());
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, SAFETY_TTL_DAYS, TimeUnit.DAYS);
        }
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
