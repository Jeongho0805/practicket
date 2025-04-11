package com.example.ticketing.chat.application;

import com.example.ticketing.chat.ChatTemp;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatResponseDto {
    private String key;
    private String name;
    private String text;
    private LocalDateTime sendAt;

    public static ChatResponseDto createFromChat(ChatTemp chatTemp) {
        ChatResponseDto dto = new ChatResponseDto();
        dto.key = chatTemp.getKey();
        dto.name = chatTemp.getName();
        dto.text = chatTemp.getText();
        dto.sendAt = chatTemp.getCreatedAt();
        return dto;
    }
}
