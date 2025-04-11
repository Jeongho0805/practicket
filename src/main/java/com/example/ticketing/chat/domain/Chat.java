package com.example.ticketing.chat.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    private String key;

    private String name;

    private String text;

    private LocalDateTime createdAt;
}
