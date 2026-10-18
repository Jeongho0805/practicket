package com.practicket.drill.application;

import com.practicket.drill.domain.DrillActivityQuery;
import com.practicket.drill.dto.DrillActivityResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DrillActivityServiceTest {

    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final DrillActivityResponse ZERO =
            new DrillActivityResponse(0, 0, new DrillActivityResponse.Drill(0, 0), new DrillActivityResponse.Drill(0, 0),
                    new DrillActivityResponse.Drill(0, 0), new DrillActivityResponse.Drill(0, 0));

    private final DrillActivityQuery query = mock(DrillActivityQuery.class);
    private Instant now;
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZONE; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    };

    @Test
    @DisplayName("1분 안에는 다시 세지 않고, 1분이 지나면 다시 센다")
    void keepsForOneMinute() {
        when(query.since(any())).thenReturn(ZERO);
        DrillActivityService service = new DrillActivityService(query, clock);

        now = at(10, 0, 0);
        service.getToday();
        now = at(10, 0, 59);
        service.getToday();
        verify(query, times(1)).since(any());

        now = at(10, 1, 1);
        service.getToday();
        verify(query, times(2)).since(any());
    }

    @Test
    @DisplayName("자정을 넘기면 1분이 안 됐어도 새 날 기준으로 다시 센다")
    void recountsAfterMidnight() {
        when(query.since(any())).thenReturn(ZERO);
        DrillActivityService service = new DrillActivityService(query, clock);

        now = at(23, 59, 30);
        service.getToday();
        now = at(23, 59, 30).plusSeconds(40);
        service.getToday();

        verify(query).since(LocalDateTime.of(2026, 10, 18, 0, 0));
        verify(query).since(LocalDateTime.of(2026, 10, 19, 0, 0));
    }

    private Instant at(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 10, 18, hour, minute, second).toInstant(ZoneOffset.UTC);
    }
}
