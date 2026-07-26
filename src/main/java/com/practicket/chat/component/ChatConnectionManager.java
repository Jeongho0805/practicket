package com.practicket.chat.component;

import com.practicket.chat.dto.ChatResponseDto;
import lombok.Getter;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Repository
public class ChatConnectionManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final String EVENT_NAME = "chat";
    private final String PARTICIPANT_EVENT = "participants";

    public SseEmitter save(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        registerCallbacks(key, emitter);
        emitters.put(key, emitter);
        broadcastParticipantCount(); // 접속 → 참여자 수 갱신
        return emitter;
    }

    public void deleteByKey(String key) {
        emitters.remove(key);
        broadcastParticipantCount(); // 해제 → 참여자 수 갱신
    }

    /** 현재 채팅 SSE 연결 수 = 참여자 수. (단일 인스턴스 기준 — 다중 인스턴스면 인스턴스별 집계) */
    public int count() {
        return emitters.size();
    }

    /** 현재 참여자 수를 모든 연결에 push. 전송 실패는 무시(각 emitter의 onError 콜백이 정리). */
    public void broadcastParticipantCount() {
        int count = emitters.size();
        for (SseEmitter emitter : emitters.values()) {
            try {
                emitter.send(SseEmitter.event().name(PARTICIPANT_EVENT).data(count));
            } catch (Exception ignored) {
                // 아직 연결 전이거나 끊긴 emitter — 무시
            }
        }
    }

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
