package com.practicket.ticket.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class TicketToken {
    private String jwt;
    private String jti;
    private Instant expiredAt;
    private Duration ttl;
}
