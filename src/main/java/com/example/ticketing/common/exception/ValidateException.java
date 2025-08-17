package com.example.ticketing.common.exception;

public class ValidateException extends GlobalException {
    public ValidateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
