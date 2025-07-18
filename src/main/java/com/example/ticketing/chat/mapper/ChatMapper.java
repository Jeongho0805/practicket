package com.example.ticketing.chat.mapper;


import com.example.ticketing.chat.infra.ChatDocument;
import com.example.ticketing.chat.application.ChatRequestDto;
import com.example.ticketing.chat.application.ChatResponseDto;
import com.example.ticketing.chat.domain.Chat;
import com.example.ticketing.common.auth.ClientInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatMapper {

    public Chat toDomainFromDto(ClientInfo userInfo, ChatRequestDto dto) {
        return Chat.builder()
                .key(userInfo.getToken())
                .name(userInfo.getName())
                .text(dto.getText())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ChatDocument toEntityFromDomain(Chat chat) {
        return ChatDocument.builder()
                .key(chat.getKey())
                .name(chat.getName())
                .text(chat.getText())
                .createdAt(chat.getCreatedAt())
                .build();
    }

    public ChatResponseDto toDtoFromDomain(Chat chat) {
        return ChatResponseDto.builder()
                .key(chat.getKey())
                .name(chat.getName())
                .text(chat.getText())
                .sendAt(chat.getCreatedAt())
                .build();
    }

    public Chat toDomainFromEntity(ChatDocument document) {
        return Chat.builder()
                .key(document.getKey())
                .name(document.getName())
                .text(document.getText())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
