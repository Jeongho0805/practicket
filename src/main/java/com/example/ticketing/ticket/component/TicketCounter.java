package com.example.ticketing.ticket.component;

import org.springframework.stereotype.Component;

@Component
public class TicketCounter {

    private int ticketCount = 120;

    public synchronized void minusTicketCount(int count) {
        if (ticketCount - count < 0) {
            throw new RuntimeException("티켓이 모두 소진되었습니다.");
        }
        ticketCount -= count;
    }

    public boolean isAvailableCount(int count) {
        return ticketCount - count > 0;
    }

    public synchronized int getTicketCount() {
        return this.ticketCount;
    }

    public void resetCount() {
        ticketCount = 100;
    }
}
