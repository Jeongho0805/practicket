package com.practicket.drill.application;

import com.practicket.drill.domain.DrillActivityQuery;
import com.practicket.drill.dto.DrillActivityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 허브를 열 때마다 세지 않도록 1분 동안 같은 값을 돌려준다. 자정을 넘기면 바로 다시 센다. */
@Service
public class DrillActivityService {

    private static final Duration KEEP = Duration.ofMinutes(1);

    private final DrillActivityQuery drillActivityQuery;
    private final Clock clock;
    private DrillActivityResponse cached;
    private LocalDateTime expiresAt = LocalDateTime.MIN;

    @Autowired
    public DrillActivityService(DrillActivityQuery drillActivityQuery) {
        this(drillActivityQuery, Clock.systemDefaultZone());
    }

    DrillActivityService(DrillActivityQuery drillActivityQuery, Clock clock) {
        this.drillActivityQuery = drillActivityQuery;
        this.clock = clock;
    }

    public synchronized DrillActivityResponse getToday() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(expiresAt)) {
            return cached;
        }
        LocalDate today = now.toLocalDate();
        cached = drillActivityQuery.since(today.atStartOfDay());
        LocalDateTime nextMidnight = today.plusDays(1).atStartOfDay();
        expiresAt = now.plus(KEEP).isBefore(nextMidnight) ? now.plus(KEEP) : nextMidnight;
        return cached;
    }

    synchronized void clear() {
        expiresAt = LocalDateTime.MIN;
    }
}
