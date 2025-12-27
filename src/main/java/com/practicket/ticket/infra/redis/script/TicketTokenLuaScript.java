package com.practicket.ticket.infra.redis.script;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class TicketTokenLuaScript {
    private final RedisScript<Long> createToken;

    public TicketTokenLuaScript(
            @Value("classpath:redis/script/save-token.lua") Resource createTokenResource
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(createTokenResource);
        script.setResultType(Long.class);
        this.createToken = script;
    }

    public RedisScript<Long> createToken() {
        return createToken;
    }
}
