package com.practicket.common.exception;

import lombok.Getter;

@Getter
public class PracticeException extends GlobalException {

    public PracticeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
