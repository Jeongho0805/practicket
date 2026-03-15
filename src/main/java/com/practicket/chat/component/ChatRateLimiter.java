package com.practicket.chat.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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

    private final StringRedisTemplate stringRedisTemplate;

    public void validate(String token) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(TEMPBAN_KEY_PREFIX + token))) {
            throw new GlobalException(ErrorCode.CHAT_TEMP_BANNED);
        }

        String rateKey = RATE_KEY_PREFIX + token;
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);
        if (count == 1) {
            stringRedisTemplate.expire(rateKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count > MAX_COUNT) {
            applyWarning(token);
        }
    }

    private void applyWarning(String token) {
        String warnKey = WARN_KEY_PREFIX + token;
        Long warnCount = stringRedisTemplate.opsForValue().increment(warnKey);
        if (warnCount == 1) {
            stringRedisTemplate.expire(warnKey, WARN_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (warnCount >= WARN_THRESHOLD) {
            stringRedisTemplate.opsForValue().set(TEMPBAN_KEY_PREFIX + token, "1", TEMPBAN_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.delete(warnKey);
            throw new GlobalException(ErrorCode.CHAT_TEMP_BANNED);
        }
        throw new GlobalException(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);
    }
}
