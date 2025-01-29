package com.example.ticketing.ticket.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Component
public class TicketTimer {

    private LocalDateTime startTime;

    public TicketTimer() {
        int second = LocalDateTime.now().getSecond();
        if (second < 30) {
            startTime = LocalDateTime.now().withSecond(0).withNano(0);
        } else {
            startTime = LocalDateTime.now().plusMinutes(1).withSecond(0).withNano(0);
        }
    }

    public boolean isValidStartTime() {
        LocalDateTime now = LocalDateTime.now();
        log.info("예매 시작 시간 = {}", startTime.toString());
        log.info("현재 시간  = {}", now);
        return now.isAfter(startTime);
    }

    public void resetStartTime() {
        startTime = startTime.plusMinutes(1);
    }
}
