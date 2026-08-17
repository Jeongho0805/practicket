package com.practicket.practice.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.practice.component.PracticeRankCalculator;
import com.practicket.practice.component.PracticeRankCalculator.MonthlyRank;
import com.practicket.practice.component.PracticeSessionValidator;
import com.practicket.practice.component.PracticeSessionValidator.ValidatedSession;
import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.dto.PracticeStartResponse;
import com.practicket.practice.infra.persistence.PracticeRankRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 세션 소유자·타이밍 검증은 {@link PracticeSessionValidator} 로 빠져나갔다.
 * 그 규칙은 {@code PracticeSessionValidatorTest} 가 본다 — 여기서는 검증을 통과한 뒤
 * 서비스가 무엇을 저장하고 무엇을 지우는지만 본다.
 */
@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    @InjectMocks
    private PracticeService practiceService;

    @Mock
    private PracticeSessionRepository sessionRepository;

    @Mock
    private PracticeResultRepository resultRepository;

    @Mock
    private PracticeRankRepository rankRepository;

    @Mock
    private PracticeSessionValidator sessionValidator;

    @Mock
    private PracticeRankCalculator rankCalculator;

    // ============ start() ============

    @Test
    @DisplayName("start - 정상: sessionId를 반환하고 Redis에 세션을 저장한다")
    void startReturnsSessionIdAndSavesToRedis() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        PracticeType type = PracticeType.I_TICKET_OLD;

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
        PracticeResultRequest request = buildRequest("session-id", 10_000);
        givenValidatedSession(clientInfo, request, PracticeType.I_TICKET_OLD);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(any(PracticeResult.class));
        verify(sessionRepository).delete("session-id");
    }

    @Test
    @DisplayName("complete - 정상: type은 검증기가 세션에서 꺼내준 값이 저장된다")
    void completeTypeComesFromValidatedSession() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        PracticeResultRequest request = buildRequest("session-id", 10_000);
        givenValidatedSession(clientInfo, request, PracticeType.M_TICKET);
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
        PracticeResultRequest request = buildRequest("session-id", 10_000);
        givenValidatedSession(clientInfo, request, PracticeType.I_TICKET_OLD);
        ArgumentCaptor<PracticeResult> captor = ArgumentCaptor.forClass(PracticeResult.class);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("complete - 정상: 검증을 통과한 요청의 소요시간이 그대로 저장된다")
    void completeSavesValidatedDuration() {
        // given
        ClientInfo clientInfo = buildClientInfo("client-token-abc", "tester");
        PracticeResultRequest request = buildRequest("session-id", 10_000);
        givenValidatedSession(clientInfo, request, PracticeType.I_TICKET_OLD);
        ArgumentCaptor<PracticeResult> captor = ArgumentCaptor.forClass(PracticeResult.class);

        // when
        practiceService.complete(clientInfo, request);

        // then
        verify(resultRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalDurationMs()).isEqualTo(10_000);
    }

    /**
     * 검증을 통과한 상태를 만든다. 검증 자체가 무엇을 막는지는
     * {@code PracticeSessionValidatorTest} 가 본다.
     */
    private void givenValidatedSession(ClientInfo clientInfo, PracticeResultRequest request, PracticeType type) {
        ValidatedSession validated = new ValidatedSession(type, LocalDateTime.now().minusSeconds(15));
        when(sessionValidator.validate(clientInfo, request)).thenReturn(validated);
        when(rankCalculator.calculate(any(PracticeType.class), anyInt())).thenReturn(new MonthlyRank(50, 5, 10));
    }

    private ClientInfo buildClientInfo(String token, String name) {
        return ClientInfo.builder()
                .token(token)
                .name(name)
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
