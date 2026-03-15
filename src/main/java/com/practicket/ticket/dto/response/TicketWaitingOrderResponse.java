package com.practicket.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TicketWaitingOrderResponse {

    Long currentWaitingOrder;

    Long firstWaitingOrder;

    @JsonProperty("is_complete")
    boolean isComplete;

    String reservationToken;

    public TicketWaitingOrderResponse(Long currentWaitingOrder, Long firstWaitingOrder, String reservationToken) {
        this.currentWaitingOrder = currentWaitingOrder;
        this.firstWaitingOrder = firstWaitingOrder;
        this.isComplete = currentWaitingOrder == null && reservationToken != null;
        this.reservationToken = reservationToken;
    }
}
