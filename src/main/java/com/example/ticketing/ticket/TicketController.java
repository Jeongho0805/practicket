package com.example.ticketing.ticket;

import com.example.ticketing.common.auth.User;
import com.example.ticketing.common.auth.UserInfo;
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
    public ResponseEntity<?> getRankInfo() {
        List<TicketRankDto> rankInfo = ticketService.getRankInfo();
        return ResponseEntity.ok(rankInfo);
    }

    @GetMapping("/ticket")
    public ResponseEntity<List<String>> getSeatsInfo() {
        List<String> seats = ticketService.findAllSeats();
        return ResponseEntity.ok(seats);
    }

    @PostMapping("/ticket")
    public ResponseEntity<?> createTicket(@User UserInfo userInfo, @Valid @RequestBody TicketRequestDto dto) {
        ticketService.issueTicket(userInfo, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order")
    public ResponseEntity<SseEmitter> streamSse(@User UserInfo userInfo) {
        SseEmitter emitter = ticketQueueService.saveEmitter(userInfo.getKey());
        return ResponseEntity.ok(emitter);
    }

    @PostMapping("/order")
    public ResponseEntity<?> registerTicket(@User UserInfo userInfo) {
        ticketService.validateStartTime();
        ticketQueueService.saveEvent(userInfo.getKey());
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
