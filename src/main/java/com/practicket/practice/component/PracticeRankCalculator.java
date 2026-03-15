package com.practicket.practice.component;

import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.infra.persistence.PracticeResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PracticeRankCalculator {

    private final PracticeResultRepository resultRepository;

    /**
     * 이번 달 전체 기록 기준으로 현재 기록의 순위와 상위 퍼센타일을 계산한다.
     * (유저별 최고기록 기준이 아닌, 기록 전체 기준)
     */
    public MonthlyRank calculate(PracticeType type, int totalDurationMs) {
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        long betterCount = resultRepository.countRecordsBetterThan(type, monthStart, monthEnd, totalDurationMs);
        long totalRecords = resultRepository.countRecordsInMonth(type, monthStart, monthEnd);

        int myRank = (int) betterCount + 1;
        int percentile = (int) Math.ceil(myRank * 100.0 / totalRecords);

        return new MonthlyRank(percentile, myRank, (int) totalRecords);
    }

    public record MonthlyRank(int percentile, int myRank, int totalUsers) {}
}
