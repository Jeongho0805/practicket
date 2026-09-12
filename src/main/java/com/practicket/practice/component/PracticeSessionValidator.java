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

/**
 * 기록의 총 시간은 서버가 잰 값만 쓴다. 클라이언트가 보낸 총 시간은 대조에만 쓰고 버린다.
 * 결정 배경은 docs/adr/0005-연습-기록은-서버가-잰-시간으로-저장한다.md.
 */
@Component
@RequiredArgsConstructor
public class PracticeSessionValidator {

    private static final int MIN_ELAPSED_MS = 3_000;
    private static final int TIMING_TOLERANCE_MS = 2_000;
    private static final int COUNTDOWN_MS = 6_000;

    private final PracticeSessionRepository sessionRepository;

    public ValidatedSession validate(ClientInfo clientInfo, PracticeResultRequest request) {
        Map<Object, Object> session = sessionRepository.find(request.getSessionId());
        if (session.isEmpty()) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_NOT_FOUND);
        }

        if (!clientInfo.getToken().equals(sessionRepository.getClientKey(session))) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH);
        }

        // 화면을 열지 않고 start 와 complete 만 부르는 요청은 관문을 지나지 않는다.
        if (!sessionRepository.hasCheckpoint(session)) {
            throw new PracticeException(ErrorCode.PRACTICE_CHECKPOINT_MISSING);
        }

        long startAt = sessionRepository.getStartAt(session);
        int serverElapsedMs = (int) (Instant.now().toEpochMilli() - startAt) - COUNTDOWN_MS;

        if (serverElapsedMs < MIN_ELAPSED_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_TOO_FAST);
        }
        if (Math.abs(request.getTotalDurationMs() - serverElapsedMs) > TIMING_TOLERANCE_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_INVALID_TIMING);
        }

        /* 브라우저 시계는 서버보다 늦게 출발하므로(요청 왕복만큼) 구간 합이 서버 경과를 넘을 수 없다.
           넘었다면 구간 값을 지어낸 것이다. */
        int reportedSum = request.getReactionTimeMs() + request.getQueueWaitMs() + request.getCaptchaMs();
        if (reportedSum > serverElapsedMs) {
            throw new PracticeException(ErrorCode.PRACTICE_INVALID_TIMING);
        }

        PracticeType type = PracticeType.valueOf(sessionRepository.getType(session));
        LocalDateTime startedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(startAt), ZoneId.systemDefault());
        return new ValidatedSession(type, startedAt, serverElapsedMs);
    }

    public record ValidatedSession(PracticeType type, LocalDateTime startedAt, int serverElapsedMs) {

        /** 좌석 구간만 서버가 재지 않는다. 나머지를 빼서 구한다 — 그래야 구간 합이 총 시간과 맞는다. */
        public int seatSelectionMs(PracticeResultRequest request) {
            return serverElapsedMs - request.getReactionTimeMs()
                    - request.getQueueWaitMs() - request.getCaptchaMs();
        }

        public PracticeResult toResult(ClientInfo clientInfo, PracticeResultRequest request) {
            return new PracticeResult(
                    clientInfo.getToken(),
                    clientInfo.getName(),
                    type,
                    startedAt,
                    serverElapsedMs,
                    request.getReactionTimeMs(),
                    request.getQueueWaitMs(),
                    request.getCaptchaMs(),
                    seatSelectionMs(request),
                    request.getQueueInitialRank()
            );
        }
    }
}
