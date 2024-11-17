package com.example.ticketing.common;

import lombok.Getter;

@Getter
public class TicketException extends RuntimeException {

    private final ErrorCode errorCode;

    public TicketException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
