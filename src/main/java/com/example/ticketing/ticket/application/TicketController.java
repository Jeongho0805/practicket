package com.example.ticketing.ticket.application;

import com.example.ticketing.common.auth.Auth;
import com.example.ticketing.common.auth.ClientInfo;
import com.example.ticketing.ticket.dto.ServerTimeResponseDto;
import com.example.ticketing.ticket.dto.TicketRankDto;
import com.example.ticketing.ticket.dto.TicketRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    private final TicketQueueService ticketQueueService;

    @GetMapping("/rank")
    public ResponseEntity<List<TicketRankDto>> getRankInfo() {
        List<TicketRankDto> rankInfo = ticketService.getRankInfo();
        return ResponseEntity.ok(rankInfo);
    }

    @GetMapping("/ticket")
    public ResponseEntity<List<String>> getSeatsInfo() {
        List<String> seats = ticketService.findAllSeats();
        return ResponseEntity.ok(seats);
    }

    @PostMapping("/ticket")
    public ResponseEntity<?> createTicket(@Auth ClientInfo userInfo, @Valid @RequestBody TicketRequestDto dto) {
        ticketService.issueTicket(userInfo, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order")
    public ResponseEntity<SseEmitter> streamSse(@Auth ClientInfo userInfo) {
        SseEmitter emitter = ticketQueueService.saveEmitter(userInfo.getToken());
        return ResponseEntity.ok(emitter);
    }

    @PostMapping("/order")
    public ResponseEntity<?> registerTicket(@Auth ClientInfo userInfo) {
        ticketService.validateStartTime();
        ticketQueueService.saveEvent(userInfo.getToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/server-time")
    public ResponseEntity<?> getServerTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String formattedTime = now.format(formatter);
        ServerTimeResponseDto response = new ServerTimeResponseDto(formattedTime);
        return ResponseEntity.ok().body(response);
    }
}
