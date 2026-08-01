package com.practicket.practice.component;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.PracticeException;
import com.practicket.practice.component.PracticeSessionValidator.ValidatedSession;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.infra.redis.PracticeSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 세션 소유자·타이밍 검증. 원래 {@code PracticeService} 안에 있던 규칙이 이 클래스로 빠져나왔고,
 * 그때 테스트가 따라오지 않아 한동안 검증 규칙에 테스트가 없는 상태였다.
 *
 * <p>시간 계산에 카운트다운 5초가 빠진다는 점에 주의 —
 * {@code serverElapsedMs = (now - startAt) - 5000} 이라 세션을 15초 전에 만들면 경과는 10초다.
 */
@ExtendWith(MockitoExtension.class)
class PracticeSessionValidatorTest {

    private static final int COUNTDOWN_MS = 5_000;

    @InjectMocks
    private PracticeSessionValidator validator;

    @Mock
    private PracticeSessionRepository sessionRepository;

    @Test
    @DisplayName("정상: 세션의 type 과 시작 시각을 꺼내 돌려준다")
    void returnsSessionInfoWhenValid() {
        ClientInfo clientInfo = buildClientInfo("client-token-abc");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(startedSecondsAgo(15));
        when(sessionRepository.getType(session)).thenReturn(PracticeType.M_TICKET.name());

        // 서버 경과 10초. 클라이언트가 보낸 값도 10초라 허용 오차 안에 든다
        ValidatedSession result = validator.validate(clientInfo, buildRequest(sessionId, 10_000));

        assertThat(result.type()).isEqualTo(PracticeType.M_TICKET);
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    @DisplayName("세션 없음: PRACTICE_SESSION_NOT_FOUND")
    void throwsWhenSessionNotFound() {
        ClientInfo clientInfo = buildClientInfo("client-token-abc");
        when(sessionRepository.find(anyString())).thenReturn(Collections.emptyMap());

        assertThatThrownBy(() -> validator.validate(clientInfo, buildRequest("expired-id", 10_000)))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
    }

    @Test
    @DisplayName("소유자 불일치: PRACTICE_SESSION_OWNER_MISMATCH — 남의 세션으로 기록을 남길 수 없다")
    void throwsWhenOwnerMismatch() {
        ClientInfo clientInfo = buildClientInfo("my-client");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn("other-client");

        assertThatThrownBy(() -> validator.validate(clientInfo, buildRequest(sessionId, 10_000)))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH));
    }

    @Test
    @DisplayName("너무 빠른 요청: PRACTICE_TOO_FAST — 시작하자마자 완료를 부르는 건 사람이 한 게 아니다")
    void throwsWhenTooFast() {
        ClientInfo clientInfo = buildClientInfo("client-token-abc");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(startedSecondsAgo(0));

        assertThatThrownBy(() -> validator.validate(clientInfo, buildRequest(sessionId, 100)))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_TOO_FAST));
    }

    @Test
    @DisplayName("클라이언트가 보낸 소요시간이 서버 경과와 크게 다르면: PRACTICE_INVALID_TIMING")
    void throwsWhenClientDurationFarFromServerElapsed() {
        ClientInfo clientInfo = buildClientInfo("client-token-abc");
        String sessionId = "session-id";
        Map<Object, Object> session = Map.of("exists", "true");

        when(sessionRepository.find(sessionId)).thenReturn(session);
        when(sessionRepository.getClientKey(session)).thenReturn(clientInfo.getToken());
        when(sessionRepository.getStartAt(session)).thenReturn(startedSecondsAgo(15));

        // 서버 경과는 10초인데 3ms 걸렸다고 보냈다 — 허용 오차 2초를 한참 넘는다
        assertThatThrownBy(() -> validator.validate(clientInfo, buildRequest(sessionId, 3)))
                .isInstanceOf(PracticeException.class)
                .satisfies(e -> assertThat(((PracticeException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRACTICE_INVALID_TIMING));
    }

    private long startedSecondsAgo(int seconds) {
        return Instant.now().toEpochMilli() - (seconds * 1_000L);
    }

    private ClientInfo buildClientInfo(String token) {
        return ClientInfo.builder()
                .token(token)
                .name("tester")
                .build();
    }

    private PracticeResultRequest buildRequest(String sessionId, int totalDurationMs) {
        PracticeResultRequest request = new PracticeResultRequest();
        ReflectionTestUtils.setField(request, "sessionId", sessionId);
        ReflectionTestUtils.setField(request, "totalDurationMs", totalDurationMs);
        ReflectionTestUtils.setField(request, "reactionTimeMs", 2_000);
        ReflectionTestUtils.setField(request, "queueWaitMs", 5_000);
        ReflectionTestUtils.setField(request, "seatSelectionMs", 2_000);
        ReflectionTestUtils.setField(request, "queueInitialRank", 10);
        return request;
    }
}
