package com.example.ticketing.chat;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ChatResponseDto {
    private String key;
    private String name;
    private String text;
    private LocalDateTime sendAt;

    public static ChatResponseDto createFromChat(Chat chat) {
        ChatResponseDto dto = new ChatResponseDto();
        dto.key = chat.getKey();
        dto.name = chat.getName();
        dto.text = chat.getText();
        dto.sendAt = chat.getCreatedAt();
        return dto;
    }
}
