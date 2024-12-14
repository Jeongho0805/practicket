package com.example.ticketing.common;

import lombok.Getter;

@Getter
public class AuthException extends GlobalException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
