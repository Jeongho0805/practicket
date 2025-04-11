package com.example.ticketing.command;

import com.example.ticketing.chat.ChatTemp;
import com.example.ticketing.chat.infra.ChatDocument;
import com.example.ticketing.chat.infra.ChatMongoRepository;
import com.example.ticketing.chat.infra.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMigrationService {

    private final ChatRepository chatRepository;

    private final ChatMongoRepository chatMongoRepository;

    public void migrateChatting() {
        List<ChatDocument> chatDocumentsForCreate = new ArrayList<>();
        List<ChatDocument> alreadyExistDocuments = chatMongoRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Set<String> mongoUniqueKeys = alreadyExistDocuments.stream()
                .map(document -> document.getKey() + document.getName() + document.getText() + document.getCreatedAt().format(formatter))
                .collect(Collectors.toSet());
        for (ChatTemp chatTemp : chatRepository.findAll()) {
            String redisUniqueKey = chatTemp.getKey() + chatTemp.getName() + chatTemp.getText() + chatTemp.getCreatedAt().format(formatter);
            if (mongoUniqueKeys.contains(redisUniqueKey)) {
                continue;
            }
            chatDocumentsForCreate.add(ChatDocument.builder()
                    .name(chatTemp.getName())
                    .key(chatTemp.getKey())
                    .text(chatTemp.getText())
                    .createdAt(chatTemp.getCreatedAt())
                    .build()
            );
        }
        chatMongoRepository.saveAll(chatDocumentsForCreate);
    }
}
