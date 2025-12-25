package com.practicket.ticket.infra.redis;

import com.practicket.ticket.domain.TicketToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TicketTokenRepository {

    private static final String ACTIVE_TOKENS_KEY = "ticket:active-tokens";
    private static final String QUEUE_INFO_KEY = "ticket:queue:info";
    private static final String TOKEN_SUFFIX = ":token";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> saveTokenScript;

    public TicketTokenRepository(
            StringRedisTemplate redisTemplate,
            @Value("classpath:redis/script/save-token.lua") Resource saveTokenResource
    ) {
        this.redisTemplate = redisTemplate;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(saveTokenResource);
        script.setResultType(Long.class);
        this.saveTokenScript = script;
    }

    public void save(String clientKey, TicketToken token) {
        long expirationTimestamp = token.getExpiredAt().getEpochSecond();
        redisTemplate.opsForZSet().add(ACTIVE_TOKENS_KEY, token.getJti(), expirationTimestamp);
    }

    public void saveWithQueueInfo(String clientKey, TicketToken token) {
        List<String> keys = List.of(ACTIVE_TOKENS_KEY, QUEUE_INFO_KEY);
        String tokenField = clientKey + TOKEN_SUFFIX;
        long expirationTimestamp = token.getExpiredAt().getEpochSecond();

        redisTemplate.execute(
                saveTokenScript,
                keys,
                token.getJti(),
                String.valueOf(expirationTimestamp),
                tokenField,
                token.getJwt()
        );
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
