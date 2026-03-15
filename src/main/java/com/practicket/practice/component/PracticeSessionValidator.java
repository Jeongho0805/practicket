package com.practicket.practice.component;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.PracticeException;
import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.infra.redis.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PracticeSessionValidator {

    private static final int MIN_ELAPSED_MS = 3_000;
    private static final int TIMING_TOLERANCE_MS = 2_000;
    private static final int COUNTDOWN_MS = 5_000;

    private final PracticeSessionRepository sessionRepository;

    /**
     * 세션 소유자 확인 및 타이밍 검증 후 세션에서 추출한 정보를 반환한다.
     */
    public ValidatedSession validate(ClientInfo clientInfo, PracticeResultRequest request) {
        Map<Object, Object> session = sessionRepository.find(request.getSessionId());
        if (session.isEmpty()) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_NOT_FOUND);
        }

        if (!clientInfo.getToken().equals(sessionRepository.getClientKey(session))) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH);
        }

        long startAt = sessionRepository.getStartAt(session);
        int serverElapsedMs = (int) (Instant.now().toEpochMilli() - startAt) - COUNTDOWN_MS;

        if (serverElapsedMs < MIN_ELAPSED_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_TOO_FAST);
        }
        if (Math.abs(request.getTotalDurationMs() - serverElapsedMs) > TIMING_TOLERANCE_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_INVALID_TIMING);
        }

        PracticeType type = PracticeType.valueOf(sessionRepository.getType(session));
        LocalDateTime startedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(startAt), ZoneId.systemDefault());
        return new ValidatedSession(type, startedAt);
    }

    public record ValidatedSession(PracticeType type, LocalDateTime startedAt) {

        public PracticeResult toResult(ClientInfo clientInfo, PracticeResultRequest request) {
            return new PracticeResult(
                    clientInfo.getToken(),
                    clientInfo.getName(),
                    type,
                    startedAt,
                    request.getTotalDurationMs(),
                    request.getReactionTimeMs(),
                    request.getQueueWaitMs(),
                    request.getSeatSelectionMs(),
                    request.getQueueInitialRank()
            );
        }
    }
}
