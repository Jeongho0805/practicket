package com.practicket.ticket.infra.redis.script;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketQueueLuaScript {

    private final RedisScript<List> enterQueue;
    private final RedisScript<Long> removeFromQueue;

    public TicketQueueLuaScript(
            @Value("classpath:redis/script/enter-ticket-queue.lua") Resource enterQueueResource,
            @Value("classpath:redis/script/reset-ticket-queue.lua") Resource removeFromQueueResource
    ) {
        DefaultRedisScript<List> enterScript = new DefaultRedisScript<>();
        enterScript.setLocation(enterQueueResource);
        enterScript.setResultType(List.class);
        this.enterQueue = enterScript;

        DefaultRedisScript<Long> removeScript = new DefaultRedisScript<>();
        removeScript.setLocation(removeFromQueueResource);
        removeScript.setResultType(Long.class);
        this.removeFromQueue = removeScript;
    }

    public RedisScript<List> enterQueue() {
        return enterQueue;
    }

    public RedisScript<Long> removeFromQueue() {
        return removeFromQueue;
    }
}