package com.practicket.inquiry.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 하루 단위 상한(3건)은 DB 집계로 InquiryService 가 따로 검증한다 */
@Component
@RequiredArgsConstructor
public class InquiryRateLimiter {

    private static final String RATE_KEY_PREFIX = "inquiry:rate:";

    private static final long WINDOW_SECONDS = 30L;
    private static final long MAX_COUNT = 1L;

    private final RedisRateLimiter rateLimiter;

    public void validateBurst(String token) {
        rateLimiter.check(RATE_KEY_PREFIX + token,
                MAX_COUNT, WINDOW_SECONDS, ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED);
    }
}
