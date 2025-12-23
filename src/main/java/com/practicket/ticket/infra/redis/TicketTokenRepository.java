package com.practicket.ticket.infra.redis;

import com.practicket.ticket.domain.TicketToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class TicketTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "ticket:token:";

    private final StringRedisTemplate redisTemplate;

    public void save(String clientKey, TicketToken token) {
        String key = TOKEN_KEY_PREFIX + clientKey;
        redisTemplate.opsForValue().set(key, token.getJwt(), Duration.ofSeconds(token.getTtl().getSeconds()));
    }

    public String get(String clientKey) {
        String key = TOKEN_KEY_PREFIX + clientKey;
        return redisTemplate.opsForValue().get(key);
    }
}
