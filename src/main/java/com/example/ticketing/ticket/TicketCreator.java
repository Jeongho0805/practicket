package com.example.ticketing.ticket;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TicketCreator {

    private int ticketCount = 120;

    public synchronized String RegisterTicketCode() {
        ticketCount--;
        if (ticketCount < 0) {
            throw new RuntimeException("티켓이 모두 소진되었습니다.");
        }
        return UUID.randomUUID().toString().substring(0, 10);
    }

    public synchronized int getTicketCount() {
        return this.ticketCount;
    }

    public void resetCount() {
        ticketCount = 100;
    }
}
