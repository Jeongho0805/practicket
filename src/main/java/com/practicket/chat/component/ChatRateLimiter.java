package com.practicket.chat.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 넘쳤다고 바로 막지 않고 경고를 쌓는다. 경고가 5회 모이면 10분 임시 밴 */
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private static final String RATE_KEY_PREFIX = "chat:rate:";
    private static final String WARN_KEY_PREFIX = "chat:warn:";
    private static final String TEMPBAN_KEY_PREFIX = "chat:tempban:";

    private static final long WINDOW_SECONDS = 1L;
    private static final long MAX_COUNT = 2L;

    private static final long WARN_THRESHOLD = 5L;
    private static final long WARN_WINDOW_SECONDS = 600L;
    private static final long TEMPBAN_SECONDS = 600L;

    private final RedisRateLimiter rateLimiter;
    private final StringRedisTemplate stringRedisTemplate;

    public void validate(String token) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(TEMPBAN_KEY_PREFIX + token))) {
            throw new GlobalException(ErrorCode.CHAT_TEMP_BANNED);
        }

        if (rateLimiter.countWithin(RATE_KEY_PREFIX + token, WINDOW_SECONDS) > MAX_COUNT) {
            applyWarning(token);
        }
    }

    private void applyWarning(String token) {
        String warnKey = WARN_KEY_PREFIX + token;

        if (rateLimiter.countWithin(warnKey, WARN_WINDOW_SECONDS) >= WARN_THRESHOLD) {
            stringRedisTemplate.opsForValue()
                    .set(TEMPBAN_KEY_PREFIX + token, "1", TEMPBAN_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.delete(warnKey);
            throw new GlobalException(ErrorCode.CHAT_TEMP_BANNED);
        }

        throw new GlobalException(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);
    }
}
