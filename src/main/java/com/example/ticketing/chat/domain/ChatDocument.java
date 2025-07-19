package com.example.ticketing.chat.infra;


import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Document(collection = "chat")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatDocument {

    @Id
    private ObjectId id;

    private String key;

    private String name;

    private String text;

    private LocalDateTime createdAt;

    @Builder
    public ChatDocument(String key, String name, String text, LocalDateTime createdAt) {
        this.key = key;
        this.name = name;
        this.text = text;
        this.createdAt = createdAt;
    }
}