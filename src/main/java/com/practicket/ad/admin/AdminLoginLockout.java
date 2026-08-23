package com.practicket.ad.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 어드민 로그인 무차별 대입 방지 — IP당 실패 횟수 제한(Redis).
 * MAX_ATTEMPTS회 실패하면 LOCK_MINUTES 동안 잠금.
 */
@Component
@RequiredArgsConstructor
public class AdminLoginLockout {

    private static final String KEY_PREFIX = "admin:login:fail:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15L;

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isLocked(String ip) {
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + ip);
        return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    public void reset(String ip) {
        stringRedisTemplate.delete(KEY_PREFIX + ip);
    }
}
