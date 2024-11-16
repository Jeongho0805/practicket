package com.example.ticketing.ticket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TicketWaitingOrderResponse {

    int currentWaitingOrder;

    int firstWaitingOrder;

    @JsonProperty("isComplete")
    boolean isComplete;

    public TicketWaitingOrderResponse(int currentWaitingOrder, int firstWaitingOrder) {
        this.currentWaitingOrder = currentWaitingOrder;
        this.firstWaitingOrder = firstWaitingOrder;
        this.isComplete = currentWaitingOrder <= 0;
    }
}
