package com.practicket.ticket.infra.sse;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TicketSseEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(String clientKey, SseEmitter emitter) {
        emitters.put(clientKey, emitter);
    }

    public void deleteByClientKey(String clientKey) {
        emitters.remove(clientKey);
    }

    public SseEmitter get(String clientKey) {
        return emitters.get(clientKey);
    }

    public Map<String, SseEmitter> findAll() {
        return this.emitters;
    }
}
