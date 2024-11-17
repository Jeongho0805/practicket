package com.example.ticketing.ticket.component;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Component
public class TicketTimer {

    private LocalDateTime startTime;

    public TicketTimer() {
        int second = LocalDateTime.now().getSecond();
        if (second < 30) {
            startTime = LocalDateTime.now().withSecond(0);
        } else {
            startTime = LocalDateTime.now().plusMinutes(1).withSecond(0);
        }
    }

    public boolean isValidStartTime() {
        return LocalDateTime.now().isAfter(startTime);
    }

    public void resetStartTime() {
        startTime = startTime.plusMinutes(1);
    }
}
