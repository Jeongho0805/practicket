package com.practicket.ticket.dto.response;

import com.practicket.ticket.domain.Ticket;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TicketRankResponseDto {
    private String key;

    private String name;

    private String second;

    public static TicketRankResponseDto createFromTicket(Ticket ticket) {
        TicketRankResponseDto ticketRankDto = new TicketRankResponseDto();
        ticketRankDto.key = ticket.getKey();
        ticketRankDto.name = ticket.getName();
        ticketRankDto.second = reformSecond(ticket.getCreatedAt());
        return ticketRankDto;
    }

    private static String reformSecond(LocalDateTime createdAt) {
        int milli = createdAt.getNano() / 1_000_000;
        String formattedMilli = String.format("%02d", milli / 10);
        return createdAt.getSecond() + "." + formattedMilli + "초";
    }
}
