package com.practicket.ticket.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket implements Serializable {

    private String key;
    private String seat;
    private String name;
    private LocalDateTime createdAt;

    public Ticket(String key, String name, String seat) {
        this.key = key;
        this.name = name;
        this.seat = seat;
        this.createdAt = LocalDateTime.now();
    }
}
