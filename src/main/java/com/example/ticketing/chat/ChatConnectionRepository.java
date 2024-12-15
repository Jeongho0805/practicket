package com.example.ticketing.chat;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatConnectionRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(String key, SseEmitter emitter) {
        emitters.put(key, emitter);
    }

    public void deleteByKey(String key) {
        emitters.remove(key);
    }

    public Map<String, SseEmitter> findAll() {
        return this.emitters;
    }
}
