package com.example.ticketing.common;

import lombok.Getter;

@Getter
public class TicketException extends GlobalException {

    public TicketException(ErrorCode errorCode) {
        super(errorCode);
    }
}
