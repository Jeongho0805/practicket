package com.practicket.ticket.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.domain.Ticket;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TicketManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public TicketManager(@Qualifier("ticketRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Value("classpath:redis/script/check-and-create-tickets.lua")
    private Resource createTicketsScriptResource;

    private RedisScript<List> createTicketsScript;

    private static final String TICKETS_HASH = "tickets";

    @PostConstruct
    public void init() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(createTicketsScriptResource);
        script.setResultType(List.class);
        this.createTicketsScript = script;
    }

    public void deleteAll() {
        redisTemplate.delete(TICKETS_HASH);
    }

    public List<Ticket> findAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(TICKETS_HASH);

        List<Ticket> tickets = new ArrayList<>();
        for (Object value : entries.values()) {
            try {
                Ticket ticket = objectMapper.readValue(value.toString(), Ticket.class);
                tickets.add(ticket);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw new TicketException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        return tickets;
    }

    public void createTickets(String key, String name, List<String> seats) {
        List<Ticket> tickets = seats.stream()
                .map(seat -> new Ticket(key, name, seat))
                .toList();

        List<String> ticketsJson = new ArrayList<>();
        for (Ticket ticket : tickets) {
            try {
                ticketsJson.add(objectMapper.writeValueAsString(ticket));
            } catch (JsonProcessingException e) {
                throw new TicketException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        List<Long> result = redisTemplate.execute(
                createTicketsScript,
                seats,
                ticketsJson.toArray()
        );

        if (!result.isEmpty() && result.get(0) == 0) {
            throw new TicketException(ErrorCode.SEAT_ALREADY_BOOKED);
        }
    }
}
