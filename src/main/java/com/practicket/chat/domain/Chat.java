package com.practicket.chat.domain;


import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    @Id
    private ObjectId id;

    private String key;

    private String name;

    private String text;

    private LocalDateTime createdAt;

    @Builder
    public Chat(String key, String name, String text) {
        this.key = key;
        this.name = name;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}