package com.practicket.community.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 삭제 비밀번호 시도 횟수 제한. ChatRateLimiter 와 같은 Redis 패턴을 쓴다.
 *
 * 네 자리는 경우의 수가 1만 개뿐이라 기계가 전부 넣어보면 뚫린다.
 * 즉 비밀번호 자체보다 이 제한이 실제 방어선이다(Q5).
 * 기준을 토큰이 아니라 IP 로 잡는 이유는 토큰은 무제한으로 새로 발급받을 수 있기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class PostPasswordAttemptLimiter {

    private static final String FAIL_KEY_PREFIX = "post:pw:fail:";
    private static final String LOCK_KEY_PREFIX = "post:pw:lock:";

    private static final long MAX_FAILURES = 5L;
    private static final long FAIL_WINDOW_SECONDS = 600L;
    private static final long LOCK_SECONDS = 600L;

    private final StringRedisTemplate stringRedisTemplate;

    /** 잠겨 있으면 비밀번호 대조 자체를 하지 않는다. */
    public void validateNotLocked(String ip) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(LOCK_KEY_PREFIX + ip))) {
            throw new GlobalException(ErrorCode.POST_PASSWORD_LOCKED);
        }
    }

    /** 틀렸을 때 호출. 5회가 되면 10분 잠근다. */
    public void recordFailure(String ip) {
        String failKey = FAIL_KEY_PREFIX + ip;
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);

        if (failCount != null && failCount == 1L) {
            stringRedisTemplate.expire(failKey, FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (failCount != null && failCount >= MAX_FAILURES) {
            stringRedisTemplate.opsForValue().set(LOCK_KEY_PREFIX + ip, "1", LOCK_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.delete(failKey);
            throw new GlobalException(ErrorCode.POST_PASSWORD_LOCKED);
        }
    }

    /** 맞췄을 때 호출. 실패 누적을 지운다. */
    public void clearFailures(String ip) {
        stringRedisTemplate.delete(FAIL_KEY_PREFIX + ip);
    }
}
