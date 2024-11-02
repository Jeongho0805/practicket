package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketRankDto;
import com.example.ticketing.ticket.dto.TicketRequestDto;
import com.example.ticketing.ticket.dto.TicketWaitingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    private final TicketQueueService ticketQueueHelper;

    @GetMapping("/rank")
    public ResponseEntity<?> getRankInfo() {
        List<TicketRankDto> rankInfo = ticketService.getRankInfo();
        return ResponseEntity.ok(rankInfo);
    }

    @PostMapping("/ticket")
    public ResponseEntity<?> registerTicket(@RequestBody TicketRequestDto dto) {
        ticketService.validateRegisterAvailable();
        ticketQueueHelper.saveEvent(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order")
    public SseEmitter streamSse(@RequestParam("name") String name) {
        SseEmitter emitter = new SseEmitter(0L);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            TicketWaitingInfo info = ticketQueueHelper.getWaitingInfo(name);
            try {
                if (ticketService.isAlreadyIssued(name)) {
                    emitter.complete();  // SSE 연결 종료
                }
                emitter.send(SseEmitter.event().name("waiting-order").data(info));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        return emitter;
    }
}
