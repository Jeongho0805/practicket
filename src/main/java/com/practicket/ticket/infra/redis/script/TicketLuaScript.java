package com.practicket.ticket.infra.redis.script;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketLuaScript {
    private final RedisScript<List> createTickets;

    public TicketLuaScript(
            @Value("classpath:redis/script/create-ticket.lua") Resource createTicketsResource
    ) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(createTicketsResource);
        script.setResultType(List.class);
        this.createTickets = script;
    }

    public RedisScript<List> createTickets() {
        return createTickets;
    }
}
