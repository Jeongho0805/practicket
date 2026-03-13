package com.practicket.chat.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.chat.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic chatTopic;

    public void publish(ChatResponseDto data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.convertAndSend(chatTopic.getTopic(), json);
        } catch (Exception e) {
            log.error("채팅 메시지 발행 실패", e);
        }
    }
}
