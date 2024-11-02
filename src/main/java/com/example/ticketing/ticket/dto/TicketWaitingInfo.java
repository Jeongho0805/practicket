package com.example.ticketing.ticket.dto;

import lombok.Getter;

@Getter
public class TicketWaitingInfo {
    int myWaitingCount;
    int totalWaitingCount;

    public TicketWaitingInfo(int myWaitingCount, int totalWaitingCount) {
        this.myWaitingCount = myWaitingCount;
        this.totalWaitingCount = totalWaitingCount;
    }
}
