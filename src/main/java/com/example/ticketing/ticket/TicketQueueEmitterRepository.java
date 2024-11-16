package com.example.ticketing.ticket;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TicketQueueEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(String name, SseEmitter emitter) {
        emitters.put(name, emitter);
    }

    public void deleteByName(String name) {
        emitters.remove(name);
    }

    public Map<String, SseEmitter> findAll() {
        return this.emitters;
    }
}
