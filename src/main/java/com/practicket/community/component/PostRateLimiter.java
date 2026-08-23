package com.practicket.community.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 토큰과 IP 두 기준을 함께 건다. 토큰만 걸면 봇이 새로 발급받으면 그만이고,
 * IP 만 조이면 모바일 캐리어 NAT 때문에 같은 IP 의 다른 사용자가 같이 막힌다.
 */
@Component
@RequiredArgsConstructor
public class PostRateLimiter {

    private static final String TOKEN_KEY_PREFIX = "post:rate:token:";
    private static final String IP_KEY_PREFIX = "post:rate:ip:";

    private static final long TOKEN_WINDOW_SECONDS = 30L;
    private static final long TOKEN_MAX_COUNT = 1L;

    private static final long IP_WINDOW_SECONDS = 3600L;
    private static final long IP_MAX_COUNT = 30L;

    private final RedisRateLimiter rateLimiter;

    public void validate(String token, String ip) {
        // 토큰을 먼저 본다. 여기서 막히면 글이 저장되지 않아 IP 카운터를 올릴 필요가 없다
        rateLimiter.check(TOKEN_KEY_PREFIX + token,
                TOKEN_MAX_COUNT, TOKEN_WINDOW_SECONDS, ErrorCode.POST_RATE_LIMIT_TOKEN);

        rateLimiter.check(IP_KEY_PREFIX + ip,
                IP_MAX_COUNT, IP_WINDOW_SECONDS, ErrorCode.POST_RATE_LIMIT_IP);
    }
}
