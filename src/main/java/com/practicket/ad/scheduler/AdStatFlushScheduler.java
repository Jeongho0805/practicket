package com.practicket.ad.scheduler;

import com.practicket.ad.component.AdClickCounter;
import com.practicket.ad.component.AdImpressionCounter;
import com.practicket.ad.domain.BannerStatDaily;
import com.practicket.ad.domain.BannerStatDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Redis에 쌓인 ad:imp:*, ad:clk:* 카운터를 매시 정각에 banner_stat_daily로 flush하고 키를 정리한다.
 * docs/ad-admin-system.md Q4: 이벤트 로그(행 폭발) 대신 배너×날짜 1행 롤업, 매 렌더/클릭마다 DB에 쓰지 않는다.
 * 홈서버 단일 인스턴스·소수 배너 규모라 KEYS 스캔으로 충분(커지면 SCAN으로 교체).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdStatFlushScheduler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;
    private final BannerStatDailyRepository bannerStatDailyRepository;

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "adStatFlush", lockAtMostFor = "5m")
    public void flush() {
        flushCounters(AdImpressionCounter.keyPattern(), AdImpressionCounter.keyPrefix(), true);
        flushCounters(AdClickCounter.keyPattern(), AdClickCounter.keyPrefix(), false);
    }

    private void flushCounters(String pattern, String prefix, boolean isImpression) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            try {
                flushOne(key, prefix, isImpression);
            } catch (Exception e) {
                log.error("광고 통계 flush 실패: key={}, message={}", key, e.getMessage(), e);
            }
        }
    }

    private void flushOne(String key, String prefix, boolean isImpression) {
        String remainder = key.substring(prefix.length()); // "{bannerId}:{yyyyMMdd}"
        int sep = remainder.indexOf(':');
        if (sep < 0) {
            log.warn("광고 통계 키 형식이 올바르지 않음: {}", key);
            return;
        }

        Long bannerId;
        LocalDate statDate;
        try {
            bannerId = Long.parseLong(remainder.substring(0, sep));
            statDate = LocalDate.parse(remainder.substring(sep + 1), DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("광고 통계 키 파싱 실패: {}", key);
            return;
        }

        // GET 후 DELETE 하면 그 사이에 들어온 INCR이 삭제로 유실된다. GETDEL로 원자적으로 꺼낸다.
        long amount = parseLongSafe(stringRedisTemplate.opsForValue().getAndDelete(key));
        if (amount <= 0) {
            return;
        }

        try {
            BannerStatDaily stat = bannerStatDailyRepository.findByBannerIdAndStatDate(bannerId, statDate)
                    .orElseGet(() -> BannerStatDaily.builder()
                            .bannerId(bannerId)
                            .statDate(statDate)
                            .build());

            if (isImpression) {
                stat.addImpressions(amount);
            } else {
                stat.addClicks(amount);
            }
            bannerStatDailyRepository.save(stat);
        } catch (Exception e) {
            // 이미 GETDEL로 키를 비웠으므로 DB 반영 실패 시 되돌려놔야 유실되지 않는다(다음 정각에 재시도).
            stringRedisTemplate.opsForValue().increment(key, amount);
            throw e;
        }
    }

    private long parseLongSafe(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
