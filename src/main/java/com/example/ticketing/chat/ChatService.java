package com.example.ticketing.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    private final ChatConnectionRepository chatConnectionRepository;

    public void saveChat(ChatRequestDto dto) {
        Chat chat = new Chat(dto.getName(), dto.getText());
        chatRepository.save(chat);
    }

    public List<ChatResponseDto> findAllChat() {
        Iterable<Chat> chats = chatRepository.findAll();
        return StreamSupport
                .stream(chats.spliterator(), false)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Chat::getCreatedAt))
                .map((chat) -> ChatResponseDto.createFromChat(chat.getName(), chat.getText(), chat.getCreatedAt())).toList();
    }

    public SseEmitter connectChat(String name) {
        SseEmitter emitter = new SseEmitter();
        return emitter;
    }
}
