package com.example.ticketing.ticket.dto;

import com.example.ticketing.ticket.domain.Ticket;
import lombok.Getter;

@Getter
public class TicketRankDto {
    private String key;

    private String name;

    private String second;

    public static TicketRankDto createFromTicket(Ticket ticket) {
        TicketRankDto ticketRankDto = new TicketRankDto();
        ticketRankDto.key = ticket.getKey();
        ticketRankDto.name = ticket.getName();
        ticketRankDto.second = (ticket.getCreatedAt().getSecond()) + "." + String.valueOf(ticket.getCreatedAt().getNano()).substring(0, 2) + "초";
        return ticketRankDto;
    }
}
