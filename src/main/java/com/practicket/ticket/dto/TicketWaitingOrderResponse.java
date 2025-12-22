package com.practicket.ticket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TicketWaitingOrderResponse {

    Long currentWaitingOrder;

    Long firstWaitingOrder;

    @JsonProperty("is_complete")
    boolean isComplete;

    public TicketWaitingOrderResponse(Long currentWaitingOrder, Long firstWaitingOrder) {
        this.currentWaitingOrder = currentWaitingOrder;
        this.firstWaitingOrder = firstWaitingOrder;
        this.isComplete = currentWaitingOrder <= 0;
    }
}
