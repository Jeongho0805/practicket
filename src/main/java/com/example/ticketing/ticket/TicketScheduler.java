package com.example.ticketing.ticket;

import com.example.ticketing.ticket.component.TicketManager;
import com.example.ticketing.ticket.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class TicketScheduler {

    private final TicketService ticketService;

    private final TicketQueueService ticketQueueService;

    private final TicketManager ticketManager;

    private final static int AI_USER_NUMBER = 30;

    private final static int AI_USER_INTERVAL = 100;

    @Scheduled(cron = "30 * * * * *")
    public void resetTime() {
        ticketService.resetTimer();
    }

    @Scheduled(cron = "55 * * * * *")
    public void clearAllRecord() {
        ticketQueueService.deleteAllWaiting();
        ticketService.initData();
    }

    @Scheduled(cron = "0 * * * * *")
    public void activateAIUserOrder() {
        String name = "AI-User-";
        for (int i=1; i<=AI_USER_NUMBER; i++) {
            try {
                Thread.sleep(AI_USER_INTERVAL);
                ticketQueueService.saveEvent(name + i);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "7 * * * * *")
    public void activateAIUserTicket() {
        String name = "AI-User-";
        for (int i=1; i<=AI_USER_NUMBER; i++) {
            try {
                Thread.sleep(AI_USER_INTERVAL);
                ticketManager.createTicketForAI(name + i);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "* * * * * *")
    public void sendWaitingOrder() {
        ticketQueueService.sendOrderByEmitter();
    }
}
