package com.example.ticketing.chat;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatConnectionRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(String name, SseEmitter emitter) {
        emitters.put(name, emitter);
    }

    public void deleteByName(String name) {
        emitters.remove(name);
    }

    public List<String> findAll() {
        return new ArrayList<>(emitters.keySet());
    }
}
