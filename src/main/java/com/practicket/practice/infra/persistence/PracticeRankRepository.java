package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeType;

import java.util.List;

public interface PracticeRankRepository {

    List<PracticeRankEntry> findRanking(PracticeType type, PeriodType period,
                                        Integer cursorTotalDurationMs, Long cursorId,
                                        int limit);
}
