package com.example.ticketing.chat.application;

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
}
