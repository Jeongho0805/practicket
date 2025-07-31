package com.example.ticketing.chat.dto;

import com.example.ticketing.chat.domain.Chat;
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

    public static ChatResponseDto of(Chat chat) {
        return ChatResponseDto.builder()
                .key(chat.getKey())
                .name(chat.getName())
                .text(chat.getText())
                .sendAt(chat.getCreatedAt())
                .build();
    }
}
