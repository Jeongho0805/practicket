package com.practicket.common.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 기간 안에 몇 번 했는지 세는 공용 카운터. 키·문턱·기간은 부르는 쪽이 정한다.
 * 도메인별 리미터(글·댓글·신고·문의·채팅)가 전부 이걸 쓴다.
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /** 문턱을 넘으면 예외. 판단까지 맡기고 싶을 때 쓴다 */
    public void check(String key, long maxCount, long windowSeconds, ErrorCode errorCode) {
        if (countWithin(key, windowSeconds) > maxCount) {
            throw new GlobalException(errorCode);
        }
    }

    /**
     * 하나 세고 현재 횟수를 돌려준다. 넘쳤을 때 무엇을 할지는 부르는 쪽이 정한다.
     * Redis 가 답을 못 주면 0 이라 이번 요청은 통과한다 — 장애 때 서비스를 막는 쪽이 더 나쁘다.
     */
    public long countWithin(String key, long windowSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return 0L;
        }
        if (count == 1L) {
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count;
    }
}
