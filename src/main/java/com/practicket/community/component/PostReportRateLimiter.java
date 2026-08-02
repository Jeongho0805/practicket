package com.practicket.community.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * IP 기준만 건다 — 토큰은 무제한으로 새로 발급받을 수 있어 관문이 되지 않는다.
 * 같은 대상 중복은 {@code post_report} 유니크 제약이 이미 막는다.
 */
@Component
@RequiredArgsConstructor
public class PostReportRateLimiter {

    private static final String IP_KEY_PREFIX = "report:rate:ip:";

    private static final long IP_WINDOW_SECONDS = 86400L;
    private static final long IP_MAX_COUNT = 10L;

    private final RedisRateLimiter rateLimiter;

    public void validate(String ip) {
        rateLimiter.check(IP_KEY_PREFIX + ip,
                IP_MAX_COUNT, IP_WINDOW_SECONDS, ErrorCode.REPORT_RATE_LIMIT_IP);
    }
}
