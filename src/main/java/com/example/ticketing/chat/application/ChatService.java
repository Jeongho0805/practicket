package com.example.ticketing.chat.application;

import com.example.ticketing.chat.domain.Chat;
import com.example.ticketing.chat.infra.ChatConnectionManager;
import com.example.ticketing.chat.infra.ChatDocument;
import com.example.ticketing.chat.infra.ChatMongoRepository;
import com.example.ticketing.chat.mapper.ChatMapper;
import com.example.ticketing.common.auth.ClientInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMongoRepository chatMongoRepository;

    private final ChatConnectionManager chatConnectionStore;

    private final ChatMapper chatMapper;

    public void saveChat(ClientInfo userInfo, ChatRequestDto dto) {
        Chat chat = chatMapper.toDomainFromDto(userInfo, dto);
        chatMongoRepository.save(chatMapper.toEntityFromDomain(chat));
        sendChatMessage(chatMapper.toDtoFromDomain(chat));
    }

    public List<ChatResponseDto> findAllChat(LocalDateTime dateTime) {
        List<ChatDocument> chatDocuments = chatMongoRepository.findTop50ByCreatedAtBeforeOrderByCreatedAtDesc(dateTime);
        Collections.reverse(chatDocuments);
        return chatDocuments.stream()
                .map(chatMapper::toDomainFromEntity)
                .map(chatMapper::toDtoFromDomain)
                .toList();
    }

    public SseEmitter createConnection() {
        String key = UUID.randomUUID().toString();
        return chatConnectionStore.save(key);
    }

    public void sendChatMessage(ChatResponseDto data) {
        chatConnectionStore.broadcast(data);
    }
}
