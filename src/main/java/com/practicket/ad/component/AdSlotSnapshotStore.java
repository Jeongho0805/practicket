package com.practicket.ad.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.ad.application.AdRenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 광고 스냅샷 캐시. 광고 자리가 layout/default 에 있어 모든 페이지가 이 조회를 탄다.
 *
 * 어드민이 광고 관련 값을 저장하면 refresh 로 바로 갈아끼운다. 만료를 자정으로 두는 것은
 * 배너 게재 기간이 날짜 단위라 아무도 안 건드려도 날이 바뀌면 나갈 배너가 달라지기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdSlotSnapshotStore {

    /** 담는 모양이 바뀌면 끝 숫자를 올린다. 옛 키는 제 TTL 로 사라진다 */
    private static final String CACHE_KEY = "ad:slot-snapshot:v3";

    private final AdRenderService adRenderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AdSlotSnapshot get() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, AdSlotSnapshot.class);
            }
        } catch (Exception e) {
            // 레디스가 죽어도 광고가 통째로 사라지면 안 된다
            log.warn("광고 스냅샷 캐시 조회 실패. DB 로 대신한다.", e);
            return adRenderService.build();
        }
        return refresh();
    }

    public AdSlotSnapshot refresh() {
        AdSlotSnapshot snapshot = adRenderService.build();
        try {
            stringRedisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(snapshot),
                    secondsUntilMidnight(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("광고 스냅샷 캐시 저장 실패", e);
        }
        return snapshot;
    }

    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        return Math.max(1, Duration.between(now, midnight).getSeconds());
    }
}
