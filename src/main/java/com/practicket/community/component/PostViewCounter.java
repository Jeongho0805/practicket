package com.practicket.community.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 조회수 중복 체크. {@code ArtView} 처럼 (글, 사람) 행을 DB 에 쌓지 않고 TTL 키로 대신한다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCounter {

    private static final String KEY_PREFIX = "post:view:";
    private static final long TTL_HOURS = 24L;

    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 장애 시 false — 열어두면 새로고침마다 조회수가 올라 숫자가 영구히 오염된다 */
    public boolean markViewed(Long postId, Long clientId) {
        String key = KEY_PREFIX + postId + ":" + clientId;
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue().setIfAbsent(key, "1", TTL_HOURS, TimeUnit.HOURS));
        } catch (Exception e) {
            log.warn("조회수 중복 체크 실패. 이번 조회는 세지 않는다. postId={}", postId, e);
            return false;
        }
    }
}
