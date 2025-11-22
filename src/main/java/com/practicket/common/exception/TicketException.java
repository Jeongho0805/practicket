package com.practicket.common.exception;

import lombok.Getter;

@Getter
public class TicketException extends GlobalException {

    public TicketException(ErrorCode errorCode) {
        super(errorCode);
    }
}
