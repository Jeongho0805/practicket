package com.example.ticketing.chat.component;

import com.example.ticketing.chat.domain.Chat;
import com.example.ticketing.chat.domain.ChatMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatManager {

    private final ChatMongoRepository chatMongoRepository;

    public Chat save(String key, String name, String text) {
        Chat chat = Chat.builder()
                .key(key)
                .name(name)
                .text(text)
                .build();
        chatMongoRepository.save(chat);
        return chat;
    }

    public List<Chat> findAllByDatetime(LocalDateTime before) {
        List<Chat> chats = chatMongoRepository.findTop50ByCreatedAtBeforeOrderByCreatedAtDesc(before);
        return chats;
    }
}
