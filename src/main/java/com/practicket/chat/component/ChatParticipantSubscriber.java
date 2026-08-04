package com.practicket.chat.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 어느 파드에서든 연결이 바뀌면 모든 파드가 이 알림을 받아 자기 연결에 새 인원을 push 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatParticipantSubscriber {

    private final ChatConnectionManager chatConnectionManager;
    private final ChatParticipantCounter chatParticipantCounter;

    public void onMessage(String message) {
        try {
            chatConnectionManager.pushParticipantCount(chatParticipantCounter.total());
        } catch (Exception e) {
            log.error("참여자 수 갱신 처리 실패", e);
        }
    }
}
