package com.example.ticketing.chat.application;

import com.example.ticketing.chat.dto.ChatRequestDto;
import com.example.ticketing.chat.dto.ChatResponseDto;
import com.example.ticketing.common.auth.Auth;
import com.example.ticketing.common.auth.ClientInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<?> findAllChat(
            @RequestParam("cursor")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursor
    ) {
        List<ChatResponseDto> chats = chatService.findAllChat(cursor);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<?> sendChat(@Auth ClientInfo clientInfo, @Valid @RequestBody ChatRequestDto dto) {
        chatService.saveChat(clientInfo, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/connection", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connectChat() {
        SseEmitter emitter = chatService.createConnection();
        return ResponseEntity.ok(emitter);
    }
}
