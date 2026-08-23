package com.practicket.ad.application;

import com.practicket.ad.application.AdminAdStatService.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대시보드 기간 정규화. 조건이 여러 겹이라(프리셋/직접지정, 뒤집힘, 미래, 상한)
 * 화면으로는 다 확인할 수 없어 여기서 못박는다.
 */
@ExtendWith(MockitoExtension.class)
class AdminAdStatServicePeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 26);

    @InjectMocks
    AdminAdStatService service;

    @Test
    @DisplayName("아무 파라미터가 없으면 오늘을 포함한 최근 14일이다.")
    void defaultsToFourteenDays() {
        Period period = service.normalizePeriod(null, null, null, TODAY);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.days()).isEqualTo(14);
        assertThat(period.preset()).isEqualTo(14);
    }

    @Test
    @DisplayName("프리셋 일수는 오늘을 마지막 날로 삼는다.")
    void presetEndsToday() {
        Period period = service.normalizePeriod(30, null, null, TODAY);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 6, 27));
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.days()).isEqualTo(30);
        assertThat(period.preset()).isEqualTo(30);
    }

    @Test
    @DisplayName("버튼에 없는 일수로 들어오면 기간은 적용하되 프리셋 선택 표시는 하지 않는다.")
    void unknownWindowIsNotMarkedAsPreset() {
        Period period = service.normalizePeriod(45, null, null, TODAY);

        assertThat(period.days()).isEqualTo(45);
        assertThat(period.preset()).isNull();
    }

    @Test
    @DisplayName("days가 0이나 음수면 기본값으로 되돌린다.")
    void nonPositiveDaysFallsBack() {
        assertThat(service.normalizePeriod(0, null, null, TODAY).days()).isEqualTo(14);
        assertThat(service.normalizePeriod(-5, null, null, TODAY).days()).isEqualTo(14);
    }

    @Test
    @DisplayName("직접 지정이 프리셋보다 우선한다.")
    void explicitRangeWinsOverPreset() {
        Period period = service.normalizePeriod(
                90, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), TODAY);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(period.to()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(period.preset()).isNull();
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 서로 바꾼다 — 에러로 튕기지 않는다.")
    void swapsReversedRange() {
        Period period = service.normalizePeriod(
                null, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10), TODAY);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(period.to()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    @DisplayName("종료일이 미래면 오늘로 자른다 — 아직 없는 데이터를 그릴 이유가 없다.")
    void clampsFutureEnd() {
        Period period = service.normalizePeriod(
                null, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 31), TODAY);

        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    @DisplayName("기간 전체가 미래면 오늘 하루로 좁혀진다.")
    void collapsesFullyFutureRange() {
        Period period = service.normalizePeriod(
                null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), TODAY);

        assertThat(period.from()).isEqualTo(TODAY);
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.days()).isEqualTo(1);
    }

    @Test
    @DisplayName("365일을 넘기면 종료일 기준으로 365일까지만 본다.")
    void clampsOverlyLongRange() {
        Period period = service.normalizePeriod(
                null, LocalDate.of(2020, 1, 1), TODAY, TODAY);

        assertThat(period.days()).isEqualTo(365);
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.from()).isEqualTo(TODAY.minusDays(364));
    }

    @Test
    @DisplayName("프리셋도 365일을 넘길 수 없다.")
    void clampsOverlyLongPreset() {
        assertThat(service.normalizePeriod(9999, null, null, TODAY).days()).isEqualTo(365);
    }

    @Test
    @DisplayName("한쪽 날짜만 오면 직접 지정으로 보지 않고 프리셋으로 처리한다.")
    void halfSpecifiedRangeFallsBackToPreset() {
        Period onlyFrom = service.normalizePeriod(7, LocalDate.of(2026, 7, 1), null, TODAY);
        Period onlyTo = service.normalizePeriod(7, null, LocalDate.of(2026, 7, 10), TODAY);

        assertThat(onlyFrom.days()).isEqualTo(7);
        assertThat(onlyFrom.preset()).isEqualTo(7);
        assertThat(onlyTo.days()).isEqualTo(7);
        assertThat(onlyTo.preset()).isEqualTo(7);
    }

    @Test
    @DisplayName("같은 날 하루만 지정할 수 있다.")
    void singleDayRange() {
        Period period = service.normalizePeriod(
                null, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), TODAY);

        assertThat(period.days()).isEqualTo(1);
    }
}
