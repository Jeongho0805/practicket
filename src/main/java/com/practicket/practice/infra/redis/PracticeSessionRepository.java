package com.practicket.practice.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PracticeSessionRepository {

    private static final String SESSION_KEY_PREFIX = "practice:session:";
    private static final String FIELD_CLIENT_KEY = "clientKey";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_START_AT = "startAt";
    private static final String FIELD_CHECKPOINT_AT = "checkpointAt";
    private static final Duration TTL = Duration.ofSeconds(180);

    private final StringRedisTemplate redisTemplate;

    public void create(String sessionId, String clientKey, String type, long startAt) {
        String key = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                FIELD_CLIENT_KEY, clientKey,
                FIELD_TYPE, type,
                FIELD_START_AT, String.valueOf(startAt)
        ));
        redisTemplate.expire(key, TTL);
    }

    /**
     * 뒤로가기로 좌석 화면에 다시 들어오면 관문이 두 번 찍힌다.
     * 덮어쓰면 좌석 구간이 그만큼 짧아지므로 처음 값만 남긴다.
     */
    public void markCheckpoint(String sessionId, long checkpointAt) {
        redisTemplate.opsForHash().putIfAbsent(
                SESSION_KEY_PREFIX + sessionId, FIELD_CHECKPOINT_AT, String.valueOf(checkpointAt));
    }

    public Map<Object, Object> find(String sessionId) {
        return redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + sessionId);
    }

    public void delete(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }

    public String getClientKey(Map<Object, Object> session) {
        return (String) session.get(FIELD_CLIENT_KEY);
    }

    public String getType(Map<Object, Object> session) {
        return (String) session.get(FIELD_TYPE);
    }

    public long getStartAt(Map<Object, Object> session) {
        return Long.parseLong((String) session.get(FIELD_START_AT));
    }

    public boolean hasCheckpoint(Map<Object, Object> session) {
        return session.get(FIELD_CHECKPOINT_AT) != null;
    }
}
