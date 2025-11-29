package com.practicket.ticket.domain;

import com.practicket.ticket.dto.TicketQueueEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketQueueEventRepository {
        private final String QUEUE_NAME = "ticketQueue";

        private final RedisTemplate<String, Object> redisTemplate;

        public List<TicketQueueEventDto> findAllEvents() {
            List<Object> list = redisTemplate.opsForList().range(QUEUE_NAME, 0, -1);
            assert list != null;
            return list.stream()
                    .map(t -> (TicketQueueEventDto) t)
                    .toList();
        }

        public void pushEvent(TicketQueueEventDto dto) {
            redisTemplate.opsForList().rightPush(QUEUE_NAME, dto);
        }

        public TicketQueueEventDto pollEvent () {
            return (TicketQueueEventDto) redisTemplate.opsForList().leftPop(QUEUE_NAME);
        }

        public void deleteAll() {
            redisTemplate.delete(QUEUE_NAME);
        }

    public int getListSize() {
        List<Object> list = redisTemplate.opsForList().range(QUEUE_NAME, 0, -1);
        return list.size();
    }
}
