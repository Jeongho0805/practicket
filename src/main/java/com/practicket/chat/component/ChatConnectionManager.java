package com.practicket.chat.component;

import com.practicket.chat.dto.ChatResponseDto;
import lombok.Getter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Repository
public class ChatConnectionManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final String EVENT_NAME = "chat";

    public SseEmitter save(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        registerCallbacks(key, emitter);
        emitters.put(key, emitter);
        return emitter;
    }

    public void deleteByKey(String key) {
        emitters.remove(key);
    }

    @Async("chatTaskExecutor")
    public void broadcast(ChatResponseDto data) {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name(EVENT_NAME).data(data));
            } catch (Exception e) {
                emitters.remove(entry.getKey());
            }
        }
    }

    public void registerCallbacks(String key, SseEmitter emitter) {
        emitter.onCompletion(() -> deleteByKey(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            deleteByKey(key);
        });
        emitter.onError((error) -> {
            emitter.complete();
            deleteByKey(key);
        });
    }
}
