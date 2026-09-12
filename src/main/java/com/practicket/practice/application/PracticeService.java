package com.practicket.practice.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.PracticeException;
import com.practicket.practice.component.PracticeRankCalculator;
import com.practicket.practice.component.PracticeRankCalculator.MonthlyRank;
import com.practicket.practice.component.PracticeSessionValidator;
import com.practicket.practice.component.PracticeSessionValidator.ValidatedSession;
import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeCompleteResponse;
import com.practicket.practice.dto.PracticeMyRecordsResponse;
import com.practicket.practice.dto.PracticeMyRankResponse;
import com.practicket.practice.dto.PracticeMyStatsResponse;
import com.practicket.practice.dto.PracticeRankItem;
import com.practicket.practice.dto.PracticeRankResponse;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.dto.PracticeStartResponse;
import com.practicket.practice.domain.PracticeBestResult;
import com.practicket.practice.infra.persistence.PracticeBestResultRepository;
import com.practicket.practice.infra.persistence.PracticeResultRepository;
import com.practicket.practice.infra.redis.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeSessionRepository sessionRepository;
    private final PracticeResultRepository resultRepository;
    private final PracticeBestResultRepository bestResultRepository;
    private final PracticeSessionValidator sessionValidator;
    private final PracticeRankCalculator rankCalculator;

    public PracticeStartResponse start(ClientInfo clientInfo, PracticeType type) {
        if (clientInfo.getName() == null || clientInfo.getName().isBlank()) {
            throw new PracticeException(ErrorCode.NICKNAME_REQUIRED);
        }
        String sessionId = UUID.randomUUID().toString();
        sessionRepository.create(sessionId, clientInfo.getToken(), type.name(), Instant.now().toEpochMilli());
        return new PracticeStartResponse(sessionId);
    }

    /**
     * 대기열을 통과한 순간 브라우저가 한 번 부른다. 응답으로 내려주는 값은 없다 —
     * 통과했다는 사실은 세션에만 남고, complete 가 그 세션을 본다.
     */
    public void checkpoint(ClientInfo clientInfo, String sessionId) {
        var session = sessionRepository.find(sessionId);
        if (session.isEmpty()) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_NOT_FOUND);
        }
        if (!clientInfo.getToken().equals(sessionRepository.getClientKey(session))) {
            throw new PracticeException(ErrorCode.PRACTICE_SESSION_OWNER_MISMATCH);
        }
        sessionRepository.markCheckpoint(sessionId, Instant.now().toEpochMilli());
    }

    public PracticeCompleteResponse complete(ClientInfo clientInfo, PracticeResultRequest request) {
        ValidatedSession vs = sessionValidator.validate(clientInfo, request);

        PracticeResult saved = resultRepository.save(vs.toResult(clientInfo, request));
        recordBestResult(saved);
        sessionRepository.delete(request.getSessionId());

        MonthlyRank rank = rankCalculator.calculate(vs.type(), vs.serverElapsedMs());

        return new PracticeCompleteResponse(
                vs.serverElapsedMs(),
                request.getReactionTimeMs(),
                request.getQueueWaitMs(),
                request.getCaptchaMs(),
                vs.seatSelectionMs(request),
                request.getQueueInitialRank(),
                rank.percentile(),
                rank.myRank(),
                rank.totalUsers()
        );
    }

    @Transactional(readOnly = true)
    public PracticeRankResponse getRanking(PracticeType type, PeriodType period,
                                           Integer cursorTotalDurationMs, Long cursorId,
                                           int limit) {
        List<PracticeBestResult> entries = bestResultRepository.findRanking(
                type, period, cursorTotalDurationMs, cursorId, limit + 1);

        boolean hasNext = entries.size() > limit;
        List<PracticeBestResult> data = hasNext ? entries.subList(0, limit) : entries;

        List<PracticeRankItem> items = data.stream()
                .map(best -> new PracticeRankItem(best.getNickname(), best.getTotalDurationMs(),
                        best.getReactionTimeMs(), best.getQueueWaitMs(),
                        best.getCaptchaMs(), best.getSeatSelectionMs()))
                .toList();

        if (!hasNext) {
            return new PracticeRankResponse(items, null, false);
        }

        PracticeBestResult last = data.get(data.size() - 1);
        return new PracticeRankResponse(items,
                new PracticeRankResponse.NextCursor(last.getTotalDurationMs(), last.getResultId()), true);
    }

    @Transactional(readOnly = true)
    public PracticeMyRankResponse getMyRank(ClientInfo clientInfo, PracticeType type, PeriodType period) {
        long totalUsers = bestResultRepository.countParticipants(type, period);

        return bestResultRepository.findMyBest(type, period, clientInfo.getToken())
                .map(best -> new PracticeMyRankResponse(
                        bestResultRepository.countFasterThan(type, period, best.getTotalDurationMs()) + 1,
                        best.getNickname(),
                        best.getTotalDurationMs(),
                        best.getReactionTimeMs(),
                        best.getQueueWaitMs(),
                        best.getCaptchaMs(),
                        best.getSeatSelectionMs(),
                        totalUsers))
                .orElseGet(() -> new PracticeMyRankResponse(null, null, null, null, null, null, null, totalUsers));
    }

    /**
     * 등급·상위 %·내 순위는 여기서 내지 않는다. 등급은 전체 기간 분포의 컷으로, 상위 %·순위는
     * 전체 기간 {@link #getMyRank} 로 화면이 낸다. 그래프도 같은 두 값을 읽는다.
     */
    @Transactional(readOnly = true)
    public PracticeMyStatsResponse getMyStats(ClientInfo clientInfo, PracticeType type) {
        String clientKey = clientInfo.getToken();

        long totalCount = resultRepository.countByClientKeyAndType(clientKey, type);
        if (totalCount == 0) {
            return new PracticeMyStatsResponse(null, 0);
        }

        return new PracticeMyStatsResponse(
                resultRepository.findBestMs(clientKey, type).orElse(null), (int) totalCount);
    }

    @Transactional(readOnly = true)
    public PracticeMyRecordsResponse getMyRecords(ClientInfo clientInfo, PracticeType type,
                                                   Long cursorId, int limit) {
        String clientKey = clientInfo.getToken();
        PageRequest pageable = PageRequest.of(0, limit + 1);

        List<PracticeResult> results = cursorId == null
                ? resultRepository.findByClientKeyAndTypeOrderByIdDesc(clientKey, type, pageable)
                : resultRepository.findRecordsBeforeCursor(clientKey, type, cursorId, pageable);

        boolean hasNext = results.size() > limit;
        List<PracticeResult> data = hasNext ? results.subList(0, limit) : results;

        List<PracticeMyRecordsResponse.RecordItem> items = data.stream()
                .map(r -> new PracticeMyRecordsResponse.RecordItem(
                        r.getTotalDurationMs(),
                        r.getReactionTimeMs(),
                        r.getQueueWaitMs(),
                        r.getCaptchaMs(),
                        r.getSeatSelectionMs(),
                        r.getQueueInitialRank(),
                        r.getStartedAt()
                ))
                .toList();

        Long nextCursor = hasNext ? data.get(data.size() - 1).getId() : null;
        return new PracticeMyRecordsResponse(items, nextCursor, hasNext);
    }

    /**
     * 버킷 기준은 완료 시각이 아니라 시작 시각이다. 자정을 걸쳐 끝난 기록이
     * 랭킹에서 사라지지 않으려면 조회 조건과 같은 값을 봐야 한다.
     */
    private void recordBestResult(PracticeResult result) {
        LocalDate day = result.getStartedAt().toLocalDate();
        LocalDate daily = PeriodType.DAILY.bucketStart(day);
        LocalDate weekly = PeriodType.WEEKLY.bucketStart(day);
        LocalDate monthly = PeriodType.MONTHLY.bucketStart(day);
        LocalDate allTime = PeriodType.ALL_TIME.bucketStart(day);

        bestResultRepository.insertBucketsIfAbsent(
                result.getType().name(), daily, weekly, monthly, allTime,
                result.getClientKey(), result.getNickname(), result.getId(),
                result.getTotalDurationMs(), result.getReactionTimeMs(),
                result.getQueueWaitMs(), result.getCaptchaMs(), result.getSeatSelectionMs());

        bestResultRepository.updateBucketsIfFaster(
                result.getType().name(), daily, weekly, monthly, allTime,
                result.getClientKey(), result.getNickname(), result.getId(),
                result.getTotalDurationMs(), result.getReactionTimeMs(),
                result.getQueueWaitMs(), result.getCaptchaMs(), result.getSeatSelectionMs());
    }
}
