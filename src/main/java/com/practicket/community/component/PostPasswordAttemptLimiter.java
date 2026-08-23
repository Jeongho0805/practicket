package com.practicket.community.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 삭제 비밀번호 시도 제한. 네 자리는 1만 개뿐이라 이 제한이 실제 방어선이다.
 * IP 와 글 둘 다 세는 이유는 IP 만 세면 회선을 바꿔 카운터를 초기화할 수 있어서다.
 */
@Component
@RequiredArgsConstructor
public class PostPasswordAttemptLimiter {

    private static final String IP_FAIL_KEY_PREFIX = "post:pw:fail:ip:";
    private static final String IP_LOCK_KEY_PREFIX = "post:pw:lock:ip:";

    private static final String POST_FAIL_KEY_PREFIX = "post:pw:fail:post:";
    private static final String POST_LOCK_KEY_PREFIX = "post:pw:lock:post:";

    private static final long IP_MAX_FAILURES = 5L;
    private static final long IP_FAIL_WINDOW_SECONDS = 600L;
    private static final long IP_LOCK_SECONDS = 600L;

    // 글 기준이 더 빡빡한 이유: 본인은 토큰으로 지우므로 이 경로를 탈 일이 거의 없다
    private static final long POST_MAX_FAILURES = 3L;
    private static final long POST_FAIL_WINDOW_SECONDS = 3600L;
    private static final long POST_LOCK_SECONDS = 3600L;

    private final StringRedisTemplate stringRedisTemplate;

    public void validateNotLocked(Long postId, String ip) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(IP_LOCK_KEY_PREFIX + ip))
                || Boolean.TRUE.equals(stringRedisTemplate.hasKey(POST_LOCK_KEY_PREFIX + postId))) {
            throw new GlobalException(ErrorCode.POST_PASSWORD_LOCKED);
        }
    }

    public void recordFailure(Long postId, String ip) {
        boolean postLocked = countFailure(
                POST_FAIL_KEY_PREFIX + postId, POST_LOCK_KEY_PREFIX + postId,
                POST_MAX_FAILURES, POST_FAIL_WINDOW_SECONDS, POST_LOCK_SECONDS);

        boolean ipLocked = countFailure(
                IP_FAIL_KEY_PREFIX + ip, IP_LOCK_KEY_PREFIX + ip,
                IP_MAX_FAILURES, IP_FAIL_WINDOW_SECONDS, IP_LOCK_SECONDS);

        // 둘 다 센 뒤에 던진다. 먼저 던지면 나머지 카운터가 안 올라간다
        if (postLocked || ipLocked) {
            throw new GlobalException(ErrorCode.POST_PASSWORD_LOCKED);
        }
    }

    public void clearFailures(Long postId, String ip) {
        stringRedisTemplate.delete(IP_FAIL_KEY_PREFIX + ip);
        stringRedisTemplate.delete(POST_FAIL_KEY_PREFIX + postId);
    }

    /** 문턱을 넘었으면 잠그고 true */
    private boolean countFailure(String failKey, String lockKey,
                                 long maxFailures, long windowSeconds, long lockSeconds) {
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failCount == null) {
            return false;
        }

        if (failCount == 1L) {
            stringRedisTemplate.expire(failKey, windowSeconds, TimeUnit.SECONDS);
        }

        if (failCount >= maxFailures) {
            stringRedisTemplate.opsForValue().set(lockKey, "1", lockSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.delete(failKey);
            return true;
        }

        return false;
    }
}
