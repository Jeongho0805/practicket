package com.practicket.chat.component;

import com.practicket.chat.dto.ChatResponseDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이 파드에 붙은 SSE 연결만 들고 있다. 전체 인원 집계는 {@link ChatParticipantCounter} 가 맡는다.
 */
@Getter
@Repository
@RequiredArgsConstructor
public class ChatConnectionManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final String EVENT_NAME = "chat";
    private final String PARTICIPANT_EVENT = "participants";

    private final ChatParticipantCounter participantCounter;

    public SseEmitter save(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        registerCallbacks(key, emitter);
        emitters.put(key, emitter);
        participantCounter.report(emitters.size());
        return emitter;
    }

    /**
     * 실제로 지워졌을 때만 알린다. onTimeout/onError 가 complete() 를 부르면 onCompletion 이 또 돌아
     * 한 번의 종료에 두 번 들어오는데, 그때마다 알리면 전 파드가 헛되이 SSE 를 다시 쏜다.
     */
    public void deleteByKey(String key) {
        if (emitters.remove(key) != null) {
            participantCounter.report(emitters.size());
        }
    }

    /**
     * 유휴 연결을 끊는 프록시를 피하려 SSE 주석(:)을 흘린다. 브라우저는 이 줄을 버린다.
     * 전송 실패가 곧 끊긴 연결이므로 여기서 함께 정리한다 — 끊김을 달리 알아낼 방법이 없다.
     * 여러 개가 죽어도 인원 보고는 한 번만 한다(죽은 수만큼 전 파드가 방송하면 그게 또 낭비다).
     */
    public void sendKeepAlive() {
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                dead.add(entry.getKey());
            }
        }
        if (dead.isEmpty()) {
            return;
        }
        dead.forEach(emitters::remove);
        participantCounter.report(emitters.size());
    }

    /**
     * 전체 참여자 수를 이 파드의 연결에 push. 다른 파드에서 연결이 바뀌어도
     * pub/sub 을 타고 여기까지 와서 숫자가 같이 움직인다.
     * 전송 실패는 무시(각 emitter의 onError 콜백이 정리).
     */
    public void pushParticipantCount(int total) {
        for (SseEmitter emitter : emitters.values()) {
            try {
                emitter.send(SseEmitter.event().name(PARTICIPANT_EVENT).data(total));
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
