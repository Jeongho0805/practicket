package com.practicket.practice.application;

import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeDistributionResponse;
import com.practicket.practice.infra.persistence.PracticeBestResultRepository;
import com.practicket.practice.infra.persistence.PracticeBestResultRepositoryCustom.HistogramBin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 전체 기간 기록 분포와 등급 컷. 사람마다 계산하지 않고 종목당 한 벌을 Redis 에 두고 나눠 쓴다.
 * <p>
 * 미리 채우는 스케줄러는 두지 않는다. practice_best_result 의 ALL_TIME 버킷이 사람당 한 줄이라
 * 계산이 인덱스 안에서 끝나므로, 캐시가 비었을 때 그 자리에서 채워도 사용자가 기다리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeDistributionService {

    private static final String CACHE_KEY_PREFIX = "practice:dist:";
    private static final String FIELD_DELIMITER = "|";
    private static final String VALUE_DELIMITER = ",";

    /** 하루 유입이 전체의 1% 미만이라 한 시간 안에 분포가 눈에 띄게 움직이지 않는다 */
    private static final long CACHE_TTL_HOURS = 1L;

    private static final int BIN_WIDTH_MS = 1000;

    /** 등급 경계. 컷 초는 여기서 매번 계산해 낸다 — 사람이 늘면 경계 초도 같이 따라가야 한다 */
    private static final List<Double> TIER_PERCENTILES = List.of(0.1, 0.3, 1.0, 5.0, 15.0, 35.0, 60.0, 85.0);

    private final PracticeBestResultRepository bestResultRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public PracticeDistributionResponse get(PracticeType type) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey(type));
            if (cached != null) {
                return parse(type, cached);
            }
        } catch (Exception e) {
            log.warn("기록 분포 캐시 조회 실패. 집계로 대신한다.", e);
            return calculate(type);
        }

        return refresh(type);
    }

    public PracticeDistributionResponse refresh(PracticeType type) {
        PracticeDistributionResponse distribution = calculate(type);
        try {
            stringRedisTemplate.opsForValue()
                    .set(cacheKey(type), serialize(distribution), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // 캐시에 못 넣어도 이번 응답은 정상이다
            log.warn("기록 분포 캐시 저장 실패", e);
        }
        return distribution;
    }

    private PracticeDistributionResponse calculate(PracticeType type) {
        long totalUsers = bestResultRepository.countParticipants(type, PeriodType.ALL_TIME);
        if (totalUsers == 0) {
            return PracticeDistributionResponse.empty(TIER_PERCENTILES);
        }

        List<Integer> cuts = TIER_PERCENTILES.stream()
                .map(percentile -> cutMs(type, totalUsers, percentile))
                .toList();

        Map<Integer, Integer> counted = bestResultRepository
                .findHistogram(type, PeriodType.ALL_TIME, BIN_WIDTH_MS).stream()
                .collect(Collectors.toMap(HistogramBin::startMs, HistogramBin::count,
                        Integer::sum, LinkedHashMap::new));
        if (counted.isEmpty()) {
            return PracticeDistributionResponse.empty(TIER_PERCENTILES);
        }

        int binStartMs = counted.keySet().iterator().next();
        int lastBinStartMs = binStartOf(cuts.get(cuts.size() - 1));

        // 사람이 하나도 없는 구간은 조회 결과에 안 나온다. 자리를 0 으로 채워야 막대 간격이 맞는다
        List<Integer> bins = new ArrayList<>();
        for (int startMs = binStartMs; startMs <= lastBinStartMs; startMs += BIN_WIDTH_MS) {
            bins.add(counted.getOrDefault(startMs, 0));
        }

        return new PracticeDistributionResponse(
                binStartMs, BIN_WIDTH_MS, bins, TIER_PERCENTILES, cuts, totalUsers);
    }

    /**
     * 상위 percentile 퍼센트의 커트라인. 절반을 넘는 컷은 뒤에서 세는 편이 걷는 거리가 짧다 —
     * 7만 명에서 상위 85% 는 앞에서 6만 칸이지만 뒤에서는 1만 칸이다.
     */
    private Integer cutMs(PracticeType type, long totalUsers, double percentile) {
        long rank = Math.max(1, Math.round(totalUsers * percentile / 100));
        boolean fromSlowest = rank * 2 > totalUsers;
        long offset = fromSlowest ? totalUsers - rank : rank - 1;

        return bestResultRepository
                .findMsAtOffset(type, PeriodType.ALL_TIME, offset, fromSlowest)
                .orElse(0);
    }

    private int binStartOf(int ms) {
        return ms / BIN_WIDTH_MS * BIN_WIDTH_MS;
    }

    private String cacheKey(PracticeType type) {
        return CACHE_KEY_PREFIX + type.name();
    }

    private String serialize(PracticeDistributionResponse distribution) {
        return String.join(FIELD_DELIMITER,
                String.valueOf(distribution.binStartMs()),
                String.valueOf(distribution.binWidthMs()),
                join(distribution.bins()),
                join(distribution.tierCutMs()),
                String.valueOf(distribution.totalUsers()));
    }

    private PracticeDistributionResponse parse(PracticeType type, String cached) {
        try {
            String[] fields = cached.split("\\" + FIELD_DELIMITER, -1);
            return new PracticeDistributionResponse(
                    Integer.parseInt(fields[0]),
                    Integer.parseInt(fields[1]),
                    split(fields[2]),
                    TIER_PERCENTILES,
                    split(fields[3]),
                    Long.parseLong(fields[4]));
        } catch (Exception e) {
            // 형식이 깨진 값 때문에 화면이 막히면 안 된다
            log.warn("기록 분포 캐시 형식이 깨졌다. 다시 집계한다.", e);
            return refresh(type);
        }
    }

    private String join(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(VALUE_DELIMITER));
    }

    private List<Integer> split(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(VALUE_DELIMITER)).map(Integer::parseInt).collect(Collectors.toList());
    }
}
