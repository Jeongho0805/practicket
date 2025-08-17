package com.example.ticketing.chat.application;

import com.example.ticketing.chat.component.ChatConnectionManager;
import com.example.ticketing.chat.component.ChatManager;
import com.example.ticketing.common.component.ProfanityValidator;
import com.example.ticketing.chat.domain.Chat;
import com.example.ticketing.chat.dto.ChatRequestDto;
import com.example.ticketing.chat.dto.ChatResponseDto;
import com.example.ticketing.common.auth.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatManager chatManager;

    private final ChatConnectionManager chatConnectionStore;

    private final ProfanityValidator profanityValidator;

    public void saveChat(ClientInfo userInfo, ChatRequestDto dto) {
        profanityValidator.validateProfanityText(dto.getText());
        Chat chat = chatManager.save(userInfo.getToken(), userInfo.getName(), dto.getText());
        ChatResponseDto chatResponseDto = ChatResponseDto.of(chat);
        sendChatMessage(chatResponseDto);
    }

    public List<ChatResponseDto> findAllChat(LocalDateTime dateTime) {
        List<Chat> chatList = chatManager.findAllByDatetime(dateTime);
        Collections.reverse(chatList);
        return chatList.stream()
                .map(ChatResponseDto::of)
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
