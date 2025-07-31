package com.example.ticketing.chat.application;

import com.example.ticketing.chat.component.ChatManager;
import com.example.ticketing.chat.dto.ChatRequestDto;
import com.example.ticketing.chat.dto.ChatResponseDto;
import com.example.ticketing.chat.component.ChatConnectionManager;
import com.example.ticketing.chat.domain.Chat;
import com.example.ticketing.common.auth.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatManager chatManager;

    private final ChatConnectionManager chatConnectionStore;

    public void saveChat(ClientInfo userInfo, ChatRequestDto dto) {
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
