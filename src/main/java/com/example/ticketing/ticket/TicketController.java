package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketRankDto;
import com.example.ticketing.ticket.dto.OrderRequestDto;
import com.example.ticketing.ticket.dto.TicketRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping("/ticket")
    public ResponseEntity<?> createTicket(@RequestBody TicketRequestDto dto) {
        ticketService.issueTicket(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order")
    public ResponseEntity<?> registerTicket(@RequestBody OrderRequestDto dto) {
        ticketService.validateStartTime();
        ticketQueueService.saveEvent(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order")
    public ResponseEntity<SseEmitter> streamSse(@RequestParam("name") String name) {
        SseEmitter emitter = ticketQueueService.saveEmitter(name);
        return ResponseEntity.ok(emitter);
    }
}
