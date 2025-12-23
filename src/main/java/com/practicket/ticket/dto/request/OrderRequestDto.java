package com.practicket.ticket.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequestDto {
    private String name;

    public OrderRequestDto(String name) {
        this.name = name;
    }
}
