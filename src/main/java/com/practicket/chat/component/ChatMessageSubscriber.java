package com.practicket.chat.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.chat.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSubscriber {

    private final ChatConnectionManager chatConnectionManager;
    private final ObjectMapper objectMapper;

    public void onMessage(String message) {
        try {
            ChatResponseDto dto = objectMapper.readValue(message, ChatResponseDto.class);
            chatConnectionManager.broadcast(dto);
        } catch (Exception e) {
            log.error("채팅 메시지 수신 처리 실패", e);
        }
    }
}
