package com.practicket.ad.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.ad.application.AdRenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 광고 스냅샷 캐시. 광고 자리가 layout/default 에 있어 모든 페이지가 이 조회를 탄다.
 *
 * 저장할 때 지우지 않는다. 어드민에서 배너를 고친 뒤 최대 1분 늦게 반영되는 것을 받아들인 대신
 * 무효화 경로가 없어 캐시가 코드 여기저기에 끌려다니지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdSlotSnapshotStore {

    private static final String CACHE_KEY = "ad:slot-snapshot";
    private static final long CACHE_TTL_SECONDS = 60L;

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
                    CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("광고 스냅샷 캐시 저장 실패", e);
        }
        return snapshot;
    }
}
