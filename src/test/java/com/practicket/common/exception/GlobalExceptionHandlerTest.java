package com.practicket.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 갈림길은 "응답을 아직 쓸 수 있는가" 하나다. 못 쓰면 null 을 돌려 스프링이 아무것도 안 쓰게 하고,
 * 쓸 수 있으면 기존 오류 응답을 그대로 낸다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletResponse committedSse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCommitted(true);
        return response;
    }

    @Test
    @DisplayName("SSE 가 끊기면 응답을 쓰지 않는다")
    void sseDisconnectIsSwallowed() {
        ResponseEntity<ErrorResponse> result =
                handler.handle(new IOException("Broken pipe"), committedSse());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("응답 전에 난 연결 오류는 500 으로 나간다")
    void disconnectBeforeCommitStillFails() {
        ResponseEntity<ErrorResponse> result =
                handler.handle(new IOException("Connection reset"), new MockHttpServletResponse());

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("쓸 수 없게 된 비동기 요청은 커밋 여부와 무관하게 무시한다")
    void asyncRequestNotUsableIsSwallowed() {
        ResponseEntity<ErrorResponse> result = handler.handle(
                new AsyncRequestNotUsableException("closed"), new MockHttpServletResponse());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("SSE 만료는 정상 종료라 응답을 쓰지 않는다")
    void sseTimeoutIsSwallowed() {
        ResponseEntity<ErrorResponse> result =
                handler.handle(new AsyncRequestTimeoutException(), committedSse());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("SSE 가 아닌 비동기 만료는 503 을 지킨다")
    void nonSseTimeoutKeepsServiceUnavailable() {
        ResponseEntity<ErrorResponse> result =
                handler.handle(new AsyncRequestTimeoutException(), new MockHttpServletResponse());

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("연결과 무관한 예외는 커밋된 SSE 에서도 500 으로 나간다")
    void realErrorIsNotSwallowed() {
        ResponseEntity<ErrorResponse> result =
                handler.handle(new IllegalStateException("boom"), committedSse());

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
