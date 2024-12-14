package com.example.ticketing.ticket.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@RedisHash(value = "Ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket implements Serializable {

    @Id
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
