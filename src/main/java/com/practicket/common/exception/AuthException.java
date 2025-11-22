package com.practicket.common.exception;

import lombok.Getter;

@Getter
public class AuthException extends GlobalException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
