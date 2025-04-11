package com.example.ticketing.common.exception;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor()
public class ErrorResponse {

    private int status;

    private String code;

    private String message;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String errorMessage) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorMessage)
                .build();
    }
}
