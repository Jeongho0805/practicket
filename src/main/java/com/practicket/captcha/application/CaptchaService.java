package com.practicket.captcha.application;

import com.practicket.captcha.component.CaptchaGlobalStatCache;
import com.practicket.captcha.component.CaptchaResultManager;
import com.practicket.captcha.dto.CaptchaCreateRequest;
import com.practicket.captcha.dto.CaptchaDistribution;
import com.practicket.captcha.dto.CaptchaGlobalStat;
import com.practicket.captcha.dto.CaptchaMyStat;
import com.practicket.captcha.dto.CaptchaRankRow;
import com.practicket.captcha.dto.CaptchaResultStatistic;
import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class CaptchaService {

    private static final int RECENT_LIMIT = 5;

    private final CaptchaResultManager captchaResultManager;

    private final CaptchaGlobalStatCache captchaGlobalStatCache;

    private final ClientManager clientManager;

    @Transactional(readOnly = true)
    public CaptchaResultStatistic getStatistic(ClientInfo clientInfo) {
        CaptchaGlobalStat global = captchaGlobalStatCache.get();
        CaptchaMyStat my = captchaResultManager.findMyStat(clientInfo.getClientId());

        if (my.getCount() == 0) {
            return emptyStatistic(global);
        }

        long fasterCount = captchaResultManager.countFasterThan(my.getLatest());

        return CaptchaResultStatistic.builder()
                .latestRank((int) fasterCount + 1)
                .latestResult(my.getLatest())
                .myAvgResult(my.getAverage())
                .totalAvgResult(global.getTotalAverage())
                .latestPercentile(percentile(fasterCount, global.getTotalCount()))
                .bestResult(my.getBest())
                .myCount(my.getCount())
                .totalCount(global.getTotalCount())
                .distribution(distribution(global, my.getLatest()))
                .recentResults(captchaResultManager.findRecentElapsedSeconds(clientInfo.getClientId(), RECENT_LIMIT))
                .topRanking(markMine(global.getTopRanking(), clientInfo.getClientId()))
                .todayPeople(global.getTodayPeople())
                .build();
    }

    public void createResult(ClientInfo clientInfo, CaptchaCreateRequest requestDto) {
        Client client = clientManager.findById(clientInfo.getClientId());
        captchaResultManager.save(client, requestDto.getElapsedTime());
        captchaGlobalStatCache.invalidate();
    }

    private CaptchaResultStatistic emptyStatistic(CaptchaGlobalStat global) {
        return CaptchaResultStatistic.builder()
                .latestRank(-1)
                .totalAvgResult(global.getTotalAverage())
                .totalCount(global.getTotalCount())
                .distribution(distribution(global, 0f))
                .recentResults(List.of())
                .topRanking(global.getTopRanking())
                .todayPeople(global.getTodayPeople())
                .build();
    }

    /** 순위 목록은 전 사용자 공용으로 캐시되므로 "내 줄" 표시는 캐시 밖에서 붙인다 */
    private List<CaptchaRankRow> markMine(List<CaptchaRankRow> ranking, Long clientId) {
        return ranking.stream()
                .map(row -> new CaptchaRankRow(
                        row.getClientId(),
                        row.getNickname(),
                        row.getElapsedSecond(),
                        Objects.equals(row.getClientId(), clientId)))
                .toList();
    }

    private CaptchaDistribution distribution(CaptchaGlobalStat global, float myResult) {
        long[] counts = global.getCounts();
        int myBin = -1;

        if (myResult > 0 && global.getBinWidth() > 0) {
            int rawBin = (int) Math.floor((myResult - global.getLowerBound()) / global.getBinWidth());
            myBin = Math.max(0, Math.min(counts.length - 1, rawBin));
        }

        return new CaptchaDistribution(global.getLowerBound(), global.getBinWidth(), counts, myBin);
    }

    private float percentile(long fasterCount, long totalCount) {
        if (totalCount == 0) {
            return 0f;
        }
        return (fasterCount * 100f) / totalCount;
    }
}
