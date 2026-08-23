package com.practicket.chat.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 파드마다 자기 연결에 보내야 하므로 ShedLock 을 걸지 않는다.
 * 15초는 SSE 스펙이 프록시 타임아웃 대비로 권하는 간격이다(앞단 nginx 는 60초에 끊는다).
 */
@Component
@RequiredArgsConstructor
public class ChatKeepAlive {

    private static final long INTERVAL_MS = 15_000;

    private final ChatConnectionManager chatConnectionManager;

    @Scheduled(fixedRate = INTERVAL_MS)
    public void send() {
        chatConnectionManager.sendKeepAlive();
    }
}
