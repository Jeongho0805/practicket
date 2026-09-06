package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeBestResult;
import com.practicket.practice.domain.PracticeType;

import java.util.List;
import java.util.Optional;

public interface PracticeBestResultRepositoryCustom {

    List<PracticeBestResult> findRanking(PracticeType type, PeriodType period,
                                         Integer cursorTotalDurationMs, Long cursorResultId,
                                         int limit);

    Optional<PracticeBestResult> findMyBest(PracticeType type, PeriodType period, String clientKey);

    long countParticipants(PracticeType type, PeriodType period);

    long countFasterThan(PracticeType type, PeriodType period, int totalDurationMs);

    /**
     * 기록 순으로 offset 번째 한 줄. 백분위 컷을 뽑는 데 쓴다.
     * <p>
     * fromSlowest 는 같은 지점을 뒤에서 세는 선택지다. 상위 85% 컷을 앞에서 세면 6만 칸을 걸어가지만
     * 뒤에서 세면 1만 칸이면 닿는다. 호출부가 절반을 넘는 컷만 뒤집어 부른다.
     */
    Optional<Integer> findMsAtOffset(PracticeType type, PeriodType period, long offset, boolean fromSlowest);

    List<HistogramBin> findHistogram(PracticeType type, PeriodType period, int binWidthMs);

    /** startMs 는 구간의 시작 밀리초다. binWidthMs 를 곱해 되돌린 값이다. */
    record HistogramBin(int startMs, int count) {
    }
}
