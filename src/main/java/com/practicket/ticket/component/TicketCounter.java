package com.practicket.ticket.component;

import com.practicket.common.exception.TicketException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.practicket.common.exception.ErrorCode.TICKET_SOLD_OUT;

@Component
@RequiredArgsConstructor
public class TicketCounter {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TICKET_COUNT_KEY = "ticket-count";
    private static final int INITIAL_TICKET_COUNT = 100;

    public void minusTicketCount(int count) {
        Long remaining = redisTemplate.opsForValue().decrement(TICKET_COUNT_KEY, count);

        if (remaining == null || remaining < 0) {
            redisTemplate.opsForValue().increment(TICKET_COUNT_KEY, count);
            throw new TicketException(TICKET_SOLD_OUT);
        }
    }

    public void plusTicketCount(int count) {
        redisTemplate.opsForValue().increment(TICKET_COUNT_KEY, count);
    }

    public void resetCount() {
        redisTemplate.opsForValue().set(TICKET_COUNT_KEY, INITIAL_TICKET_COUNT);
    }
}
