package com.practicket.common.exception;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.util.DisconnectedClientHelper;
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
    public ResponseEntity<ErrorResponse> handle(Exception e, HttpServletResponse response) {
        if (isUnwritable(e, response)) {
            return null;
        }
        return internalServerError(e);
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

    /**
     * 타임아웃은 끊긴 연결이 아니라 서버가 시간을 다 쓴 것이다. SSE 는 만료가 정상이라 삼키고,
     * 다른 비동기 응답은 스프링 기본값인 503 을 지킨다.
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handle(AsyncRequestTimeoutException e,
                                                HttpServletResponse response) {
        if (isServerSentEvent(response)) {
            return null;
        }
        ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    /**
     * 응답이 이미 나간 뒤라 무엇을 써도 스트림만 깨진다. 커밋 전이라면 아직 500 을 줄 수 있으므로
     * 삼키지 않는다 — 외부 통신이 끊긴 것도 같은 판정에 걸리기 때문이다.
     */
    private boolean isUnwritable(Exception e, HttpServletResponse response) {
        if (e instanceof AsyncRequestNotUsableException) {
            return true;
        }
        return DisconnectedClientHelper.isClientDisconnectedException(e) && response.isCommitted();
    }

    private boolean isServerSentEvent(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private ResponseEntity<ErrorResponse> internalServerError(Exception e) {
        Sentry.captureException(e);
        log.error("서버 에러 발생", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
