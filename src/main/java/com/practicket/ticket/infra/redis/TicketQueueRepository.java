package com.practicket.ticket.infra.redis;

import com.practicket.common.exception.TicketException;
import com.practicket.ticket.infra.redis.script.TicketQueueLuaScript;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.practicket.common.exception.ErrorCode.ALREADY_EXIST_WAITING_QUEUE;
import static com.practicket.common.exception.ErrorCode.INTERNAL_SERVER_ERROR;

@Repository
@RequiredArgsConstructor
public class TicketQueueRepository {

    private static final String QUEUE_KEY = "ticket:queue";
    private static final String SEQ_KEY   = "ticket:queue:seq";
    private static final String INITIAL_RANKS_KEY = "ticket:queue:info";

    private final StringRedisTemplate redisTemplate;
    private final TicketQueueLuaScript luaScript;

    public void enterQueue(String clientKey) {
        List<String> keys = List.of(QUEUE_KEY, SEQ_KEY, INITIAL_RANKS_KEY);
        List<Long> result = redisTemplate.execute(
                luaScript.enterQueue(),
                keys,
                clientKey
        );
        if (result == null || result.isEmpty()) {
            throw new TicketException(INTERNAL_SERVER_ERROR);
        }
        if (result.get(0) == 0) {
            throw new TicketException(ALREADY_EXIST_WAITING_QUEUE);
        }
    }

    public Long getCurrentRank(String clientKey) {
        return redisTemplate.opsForZSet().rank(QUEUE_KEY, clientKey);
    }

    public Long getInitialRank(String clientKey) {
        String rank = (String) redisTemplate.opsForHash().get(INITIAL_RANKS_KEY, clientKey);
        return rank != null ? Long.parseLong(rank) : null;
    }

    public String poll() {
        var poppedElement = redisTemplate.opsForZSet().popMin(QUEUE_KEY);
        return poppedElement != null ? poppedElement.getValue() : null;
    }

    public Long size() {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY);
    }

    public void deleteAll() {
        List<String> keys = List.of(QUEUE_KEY, SEQ_KEY, INITIAL_RANKS_KEY);
        redisTemplate.execute(luaScript.removeFromQueue(), keys);
    }
}

