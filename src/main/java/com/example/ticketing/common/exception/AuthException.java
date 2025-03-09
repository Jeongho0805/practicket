package com.example.ticketing.common.exception;

import lombok.Getter;

@Getter
public class AuthException extends GlobalException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
