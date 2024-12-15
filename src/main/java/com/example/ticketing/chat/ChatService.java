package com.example.ticketing.chat;

import com.example.ticketing.common.auth.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    private final ChatConnectionRepository chatConnectionRepository;

    public void saveChat(UserInfo userInfo, ChatRequestDto dto) {
        Chat chat = new Chat(userInfo.getKey(), userInfo.getName(), dto.getText());
        chatRepository.save(chat);
        sendChatMessage(ChatResponseDto.createFromChat(chat));
    }

    public List<ChatResponseDto> findAllChat() {
        Iterable<Chat> chats = chatRepository.findAll();
        return StreamSupport
                .stream(chats.spliterator(), false)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Chat::getCreatedAt))
                .map(ChatResponseDto::createFromChat)
                .toList();
    }

    public SseEmitter connectChat(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        chatConnectionRepository.save(key, emitter);
        emitter.onCompletion(() -> chatConnectionRepository.deleteByKey(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            chatConnectionRepository.deleteByKey(key);
        });
        emitter.onError((error) -> {
            emitter.complete();
            chatConnectionRepository.deleteByKey(key);
        });
        return emitter;
    }

    public void sendChatMessage(ChatResponseDto data) {
        Map<String, SseEmitter> emitters = chatConnectionRepository.findAll();
        if (emitters.isEmpty()) {
            return;
        }
        for (String key : emitters.keySet()) {
            SseEmitter emitter = emitters.get(key);
            if (emitter == null) continue;
            try{
                if (isEmitterActive(emitter)) {
                    emitter.send(SseEmitter.event()
                            .name("chat")
                            .data(data));
                }
            } catch (Exception e){
                chatConnectionRepository.deleteByKey(key);
            }
        }
    }

    private boolean isEmitterActive(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("chat"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
