package com.practicket.practice.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.PracticeException;
import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeRankItem;
import com.practicket.practice.dto.PracticeRankResponse;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.dto.PracticeStartResponse;
import com.practicket.practice.infra.persistence.PracticeRankEntry;
import com.practicket.practice.infra.persistence.PracticeRankRepository;
import com.practicket.practice.infra.persistence.PracticeResultRepository;
import com.practicket.practice.infra.redis.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PracticeService {

    private static final int MIN_ELAPSED_MS = 3_000;
    private static final int TIMING_TOLERANCE_MS = 2_000;

    private final PracticeSessionRepository sessionRepository;
    private final PracticeResultRepository resultRepository;
    private final PracticeRankRepository rankRepository;

    public PracticeStartResponse start(ClientInfo clientInfo, PracticeType type) {
        String sessionId = UUID.randomUUID().toString();
        long startAt = Instant.now().toEpochMilli();
        sessionRepository.create(sessionId, clientInfo.getToken(), type.name(), startAt);
        return new PracticeStartResponse(sessionId);
    }

    public void complete(ClientInfo clientInfo, PracticeResultRequest request) {
        Map<Object, Object> session = sessionRepository.find(request.getSessionId());
        if (session.isEmpty()) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_NOT_FOUND);
        }

        String sessionClientKey = sessionRepository.getClientKey(session);
        if (!clientInfo.getToken().equals(sessionClientKey)) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH);
        }

        long startAt = sessionRepository.getStartAt(session);
        long now = Instant.now().toEpochMilli();
        int serverElapsedMs = (int) (now - startAt);

        if (serverElapsedMs < MIN_ELAPSED_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_TOO_FAST);
        }

        int phaseSum = request.getReactionTimeMs() + request.getQueueWaitMs() + request.getSeatSelectionMs();
        if (Math.abs(phaseSum - serverElapsedMs) > TIMING_TOLERANCE_MS) {
            throw new PracticeException(ErrorCode.PRACTICE_INVALID_TIMING);
        }

        PracticeType type = PracticeType.valueOf(sessionRepository.getType(session));
        LocalDateTime startedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(startAt), ZoneId.systemDefault());

        resultRepository.save(new PracticeResult(
                clientInfo.getToken(),
                clientInfo.getName(),
                type,
                startedAt,
                serverElapsedMs,
                request.getReactionTimeMs(),
                request.getQueueWaitMs(),
                request.getSeatSelectionMs(),
                request.getQueueInitialRank()
        ));

        sessionRepository.delete(request.getSessionId());
    }

    @Transactional(readOnly = true)
    public PracticeRankResponse getRanking(PracticeType type, PeriodType period,
                                           Integer cursorTotalDurationMs, Long cursorId,
                                           int limit) {
        List<PracticeRankEntry> entries = rankRepository.findRanking(
                type, period, cursorTotalDurationMs, cursorId, limit + 1);

        boolean hasNext = entries.size() > limit;
        List<PracticeRankEntry> data = hasNext ? entries.subList(0, limit) : entries;

        List<PracticeRankItem> items = data.stream()
                .map(e -> new PracticeRankItem(e.nickname(), e.totalDurationMs()))
                .toList();

        if (!hasNext) {
            return new PracticeRankResponse(items, null, false);
        }

        PracticeRankEntry last = data.get(data.size() - 1);
        PracticeRankResponse.NextCursor nextCursor =
                new PracticeRankResponse.NextCursor(last.totalDurationMs(), last.id());

        return new PracticeRankResponse(items, nextCursor, true);
    }
}
