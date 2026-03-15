package com.practicket.ticket.infra.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.domain.Ticket;
import com.practicket.ticket.infra.redis.script.TicketLuaScript;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TicketRepository {

    private static final String TICKET_KEY = "ticket";
    private static final String ACTIVE_TOKENS_KEY = "ticket:active-tokens";

    private final ObjectMapper objectMapper;
    private final TicketLuaScript luaScript;
    private final StringRedisTemplate redisTemplate;

    public TicketRepository(
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            TicketLuaScript luaScript,
            StringRedisTemplate redisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.luaScript = luaScript;
        this.redisTemplate = redisTemplate;
    }

    public List<Ticket> findAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(TICKET_KEY);

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

    public void createTickets(String clientKey, String name, List<String> seats, String jti) {
        List<String> ticketsJson = seats.stream()
                .map(seat -> new Ticket(clientKey, name, seat))
                .map(ticket -> {
                    try {
                        return objectMapper.writeValueAsString(ticket);
                    } catch (JsonProcessingException e) {
                        throw new TicketException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
                })
                .toList();

        List<String> keys = List.of(TICKET_KEY, ACTIVE_TOKENS_KEY);
        List<String> argv = new ArrayList<>();
        argv.add(jti);
        argv.add(String.valueOf(Instant.now().getEpochSecond()));
        argv.addAll(seats);
        argv.addAll(ticketsJson);

        List<Long> result = redisTemplate.execute(
                luaScript.createTickets(),
                keys,
                argv.toArray()
        );

        if (!result.isEmpty() && result.get(0) == 0) {
            Long errorCode = result.get(1);
            if (errorCode == -2) {
                throw new TicketException(ErrorCode.TICKET_TOKEN_IS_NOT_VALID);
            }
            throw new TicketException(ErrorCode.SEAT_ALREADY_BOOKED);
        }
    }

    public void deleteAll() {
        redisTemplate.delete(TICKET_KEY);
    }
}
