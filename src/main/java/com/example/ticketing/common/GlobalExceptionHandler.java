package com.example.ticketing.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(TicketException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
