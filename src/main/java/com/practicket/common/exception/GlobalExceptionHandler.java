package com.practicket.common.exception;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(GlobalException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        Sentry.captureException(e);
        log.error("서버 에러 발생={} / 메시지={}", e.getClass().getName(), e.getMessage());
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NoResourceFoundException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.PARAMETER_IS_NOT_VALID;
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(errorCode.getMessage());
        ErrorResponse response = ErrorResponse.of(errorCode, message);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /* 아래 둘은 요청이 틀린 것이지 서버가 죽은 것이 아니다. 500 으로 두면 Sentry 에 장애로 쌓여
       진짜 장애가 묻힌다. 예외 메시지에는 클래스·필드 이름이 들어 있어 사용자에게 내보내지 않는다. */

    /** 주소 파라미터가 enum·숫자로 변환되지 않을 때 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = ErrorCode.PARAMETER_IS_NOT_VALID;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    /** 본문 JSON 을 읽지 못할 때 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException e) {
        ErrorCode errorCode = ErrorCode.PARAMETER_IS_NOT_VALID;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handle(AsyncRequestNotUsableException e) {}

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handle(MultipartException e) {
        log.warn("멀티파트 요청 파싱 실패: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.PARAMETER_IS_NOT_VALID;
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
