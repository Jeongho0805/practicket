package com.practicket.inquiry.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 문의 연타(도배) 방지 - 토큰당 30초 1건.
 * 하루 단위 상한(3건)은 DB 집계로 InquiryService에서 별도 검증한다.
 */
@Component
@RequiredArgsConstructor
public class InquiryRateLimiter {

    private static final String RATE_KEY_PREFIX = "inquiry:rate:";
    private static final long WINDOW_SECONDS = 30L;

    private final StringRedisTemplate stringRedisTemplate;

    public void validateBurst(String token) {
        String rateKey = RATE_KEY_PREFIX + token;
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(rateKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > 1L) {
            throw new GlobalException(ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED);
        }
    }
}
