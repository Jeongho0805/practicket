package com.example.ticketing.common.exception;

import lombok.Getter;

@Getter
public class TicketException extends GlobalException {

    public TicketException(ErrorCode errorCode) {
        super(errorCode);
    }
}
