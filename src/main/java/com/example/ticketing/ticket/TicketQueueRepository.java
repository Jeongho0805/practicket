package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketQueueRepository {
        private final String QUEUE_NAME = "ticketQueue";

        private final RedisTemplate<String, Object> redisTemplate;

        public List<String> findAllNameList() {
            List<Object> list = redisTemplate.opsForList().range(QUEUE_NAME, 0, -1);
            assert list != null;
            return list.stream()
                    .map(t -> (TicketRequestDto) t)
                    .map(TicketRequestDto::getName)
                    .toList();
        }

        public void pushEvent(TicketRequestDto dto) {
            redisTemplate.opsForList().rightPush(QUEUE_NAME, dto);
        }

        public TicketRequestDto pollEvent () {
            return (TicketRequestDto) redisTemplate.opsForList().leftPop(QUEUE_NAME);
        }

        public void deleteAll() {
            redisTemplate.delete(QUEUE_NAME);
        }

    public int getListSize() {
        List<Object> list = redisTemplate.opsForList().range(QUEUE_NAME, 0, -1);
        return list.size();
    }
}
