package com.practicket.community.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 토큰·IP 문턱 모두 글보다 느슨하다 — 댓글은 마찰이 작아야 달린다 */
@Component
@RequiredArgsConstructor
public class PostCommentRateLimiter {

    private static final String TOKEN_KEY_PREFIX = "comment:rate:token:";
    private static final String IP_KEY_PREFIX = "comment:rate:ip:";

    private static final long TOKEN_WINDOW_SECONDS = 10L;
    private static final long TOKEN_MAX_COUNT = 1L;

    private static final long IP_WINDOW_SECONDS = 3600L;
    private static final long IP_MAX_COUNT = 30L;

    private final RedisRateLimiter rateLimiter;

    public void validate(String token, String ip) {
        // 토큰에서 막히면 댓글이 저장되지 않아 IP 카운터를 올릴 필요가 없다
        rateLimiter.check(TOKEN_KEY_PREFIX + token,
                TOKEN_MAX_COUNT, TOKEN_WINDOW_SECONDS, ErrorCode.COMMENT_RATE_LIMIT_TOKEN);

        rateLimiter.check(IP_KEY_PREFIX + ip,
                IP_MAX_COUNT, IP_WINDOW_SECONDS, ErrorCode.COMMENT_RATE_LIMIT_IP);
    }
}
