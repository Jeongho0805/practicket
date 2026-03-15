package com.practicket.practice.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.PracticeException;
import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.dto.PracticeStartResponse;
import com.practicket.practice.infra.persistence.PracticeResultRepository;
import com.practicket.practice.infra.redis.PracticeSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    @InjectMocks
    private PracticeService practiceService;

    @Mock
    private PracticeSessionRepository sessionRepository;

    @Mock
    private PracticeResultRepository resultRepository;

    // ============ start() ============

    @Test
    @DisplayName("start - 정상: sessionId를 반환하고 Redis에 세션을 저장한다")
    void startReturnsSessionIdAndSavesToRedis() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        PracticeType type = PracticeType.I_TICKET_NEW;

        // when
        PracticeStartResponse response = practiceService.start(clientInfo, type);

        // then
        assertThat(response.getSessionId()).isNotNull();
        verify(sessionRepository).create(anyString(), eq(clientInfo.getToken()), eq(type.name()), anyLong());
    }

    // ============ complete() ============

    @Test
    @DisplayName("complete - 정상: DB에 저장하고 Redis 세션을 삭제한다")
    void completeSavesToDbAndDeletesSession() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli() - 10_000);
        when(sessionRepository.getType(session)).thenReturn(PracticeType.I_TICKET_NEW.name());

        PracticeResultRequest request = buildRequest(sessionId, 2000, 5000, 2000, 10);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(any(PracticeResult.class));
        verify(sessionRepository).delete(sessionId);
    }

    @Test
    @DisplayName("complete - 정상: totalDurationMs는 클라이언트 phaseSum이 아닌 서버 계산값이다")
    void completeTotalDurationMsIsServerCalculatedNotClientPhaseSum() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli() - 10_000);
        when(sessionRepository.getType(session)).thenReturn(PracticeType.I_TICKET_NEW.name());

        // phaseSum = 2000 + 5000 + 2000 = 9000ms, 실제 서버 경과는 ~10000ms
        PracticeResultRequest request = buildRequest(sessionId, 2000, 5000, 2000, 10);
        ArgumentCaptor<PracticeResult> captor = ArgumentCaptor.forClass(PracticeResult.class);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(captor.capture());
        PracticeResult saved = captor.getValue();
        assertThat(saved.getTotalDurationMs()).isNotEqualTo(9_000);
        assertThat(saved.getTotalDurationMs()).isGreaterThanOrEqualTo(9_000);
    }

    @Test
    @DisplayName("complete - 정상: type은 Redis 세션에서 꺼낸 값이 저장된다")
    void completeTypeIsFromRedisSession() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli() - 10_000);
        when(sessionRepository.getType(session)).thenReturn(PracticeType.M_TICKET.name());

        PracticeResultRequest request = buildRequest(sessionId, 2000, 5000, 2000, 10);
        ArgumentCaptor<PracticeResult> captor = ArgumentCaptor.forClass(PracticeResult.class);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PracticeType.M_TICKET);
    }

    @Test
    @DisplayName("complete - 정상: 그 시점의 nickname이 저장된다")
    void completeNicknameIsFromClientInfoAtThatTime() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli() - 10_000);
        when(sessionRepository.getType(session)).thenReturn(PracticeType.I_TICKET_NEW.name());

        PracticeResultRequest request = buildRequest(sessionId, 2000, 5000, 2000, 10);
        ArgumentCaptor<PracticeResult> captor = ArgumentCaptor.forClass(PracticeResult.class);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("complete - 세션 없음: PRACTICE_SESSION_NOT_FOUND 예외 발생")
    void completeThrowsExceptionWhenSessionNotFound() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        when(sessionRepository.find(anyString())).thenReturn(Collections.emptyMap());
        PracticeResultRequest request = buildRequest("expired-id", 1000, 5000, 2000, 10);

        // when & then
        assertThatThrownBy(() -> practiceService.complete(clientInfo, request))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
    }

    @Test
    @DisplayName("complete - 소유자 불일치: PRACTICE_SESSION_OWNER_MISMATCH 예외 발생")
    void completeThrowsExceptionWhenOwnerMismatch() {
        // given
        ClientInfo clientInfo = buildClientInfo("my-client", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn("other-client");

        PracticeResultRequest request = buildRequest(sessionId, 1000, 5000, 2000, 10);

        // when & then
        assertThatThrownBy(() -> practiceService.complete(clientInfo, request))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH));
    }

    @Test
    @DisplayName("complete - 너무 빠른 요청: PRACTICE_TOO_FAST 예외 발생")
    void completeThrowsExceptionWhenTooFast() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli()); // 방금 시작 → 경과 ~0ms

        PracticeResultRequest request = buildRequest(sessionId, 100, 200, 100, 10);

        // when & then
        assertThatThrownBy(() -> practiceService.complete(clientInfo, request))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_TOO_FAST));
    }

    @Test
    @DisplayName("complete - phase 합산 오차 초과: PRACTICE_INVALID_TIMING 예외 발생")
    void completeThrowsExceptionWhenInvalidTiming() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(Instant.now().toEpochMilli() - 10_000); // 서버 경과 ~10000ms

        // phaseSum = 1 + 1 + 1 = 3ms → 서버 경과(~10000ms)와 차이 ~10000ms > 허용 오차(2000ms)
        PracticeResultRequest request = buildRequest(sessionId, 1, 1, 1, 10);

        // when & then
        assertThatThrownBy(() -> practiceService.complete(clientInfo, request))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_INVALID_TIMING));
    }

    private ClientInfo buildClientInfo(String token, String name) {
        return ClientInfo.builder()
                .token(token)
                .name(name)
                .build();
    }

    private PracticeResultRequest buildRequest(String sessionId, int reactionTimeMs,
                                               int queueWaitMs, int seatSelectionMs,
                                               int queueInitialRank) {
        PracticeResultRequest request = new PracticeResultRequest();
        ReflectionTestUtils.setField(request, "sessionId", sessionId);
        ReflectionTestUtils.setField(request, "reactionTimeMs", reactionTimeMs);
        ReflectionTestUtils.setField(request, "queueWaitMs", queueWaitMs);
        ReflectionTestUtils.setField(request, "seatSelectionMs", seatSelectionMs);
        ReflectionTestUtils.setField(request, "queueInitialRank", queueInitialRank);
        return request;
    }
}
