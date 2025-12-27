package com.practicket.ticket.infra.redis;

import com.practicket.ticket.domain.TicketToken;
import com.practicket.ticket.infra.redis.script.TicketTokenLuaScript;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketTokenRepository {

    private static final String ACTIVE_TOKENS_KEY = "ticket:active-tokens";
    private static final String QUEUE_INFO_KEY = "ticket:queue:info";
    private static final String TOKEN_SUFFIX = ":token";

    private final StringRedisTemplate redisTemplate;
    private final TicketTokenLuaScript luaScript;

    public void saveWithQueueInfo(String clientKey, TicketToken token) {
        List<String> keys = List.of(ACTIVE_TOKENS_KEY, QUEUE_INFO_KEY);
        String tokenField = clientKey + TOKEN_SUFFIX;
        long expirationTimestamp = token.getExpiredAt().getEpochSecond();

        redisTemplate.execute(
                luaScript.createToken(),
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
