package com.example.ticketing.ticket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TicketRequestDto {
    private String name;

    public TicketRequestDto(String name) {
        this.name = name;
    }
}
