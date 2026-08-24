package com.practicket.chat.application;

import com.practicket.chat.component.ChatConnectionManager;
import com.practicket.chat.component.ChatManager;
import com.practicket.chat.component.ChatMessagePublisher;
import com.practicket.chat.component.ChatParticipantCounter;
import com.practicket.chat.component.ChatRateLimiter;
import com.practicket.chat.domain.Chat;
import com.practicket.chat.dto.ChatRequestDto;
import com.practicket.chat.dto.ChatResponseDto;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.component.ProfanityValidator;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatManager chatManager;
    private final ChatConnectionManager chatConnectionStore;
    private final ChatParticipantCounter chatParticipantCounter;
    private final ProfanityValidator profanityValidator;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatRateLimiter chatRateLimiter;

    public void saveChat(ClientInfo userInfo, ChatRequestDto dto) {
        if (userInfo.getBanned()) {
            throw new GlobalException(ErrorCode.CHAT_BANNED_USER);
        }
        chatRateLimiter.validate(userInfo.getToken());
        profanityValidator.validateProfanityText(dto.getText());
        Chat chat = chatManager.save(userInfo.getToken(), userInfo.getName(), dto.getText());
        ChatResponseDto chatResponseDto = ChatResponseDto.of(chat);
        chatMessagePublisher.publish(chatResponseDto);
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

    public int participantCount() {
        return chatParticipantCounter.total();
    }
}
