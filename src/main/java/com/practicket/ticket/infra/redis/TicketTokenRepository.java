package com.practicket.ticket.infra.redis;

import com.practicket.ticket.domain.TicketToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketTokenRepository {

    private static final String ACTIVE_TOKENS_KEY = "ticket:active-tokens";

    private final StringRedisTemplate redisTemplate;

    public void save(String clientKey, TicketToken token) {
        long expirationTimestamp = token.getExpiredAt().getEpochSecond();
        redisTemplate.opsForZSet().add(ACTIVE_TOKENS_KEY, token.getJti(), expirationTimestamp);
    }
    
    public long countActiveTokens() {
        Long count = redisTemplate.opsForZSet().zCard(ACTIVE_TOKENS_KEY);
        return count != null ? count : 0L;
    }
    
    public void cleanupExpiredTokens(long nowEpochSeconds) {
        redisTemplate.opsForZSet().removeRangeByScore(
            ACTIVE_TOKENS_KEY,
            Double.NEGATIVE_INFINITY,
            nowEpochSeconds
        );
    }
    
    public Long getExpirationTime(String jti) {
        Double score = redisTemplate.opsForZSet().score(ACTIVE_TOKENS_KEY, jti);
        return score != null ? score.longValue() : null;
    }
    
    public void remove(String jti) {
        redisTemplate.opsForZSet().remove(ACTIVE_TOKENS_KEY, jti);
    }
}
