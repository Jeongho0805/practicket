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
}
