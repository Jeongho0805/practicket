package com.practicket.ad.application;

import com.practicket.ad.domain.BannerStatDailyRepository;
import com.practicket.ad.domain.BannerStatSum;
import com.practicket.ad.domain.DailyStatSum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 노출·클릭 집계. banner_stat_daily는 배너×날짜 롤업이라 화면에 필요한 합산·추이를 여기서 만든다.
 * 캠페인·광고주·자리 같은 판매 구조는 알지 못한다 — 그쪽은 {@link AdminCampaignService} 가 갖고,
 * 배너 id 별 합계만 받아 간다.
 *
 * 주의: 노출·클릭은 Redis에 쌓였다가 {@code AdStatFlushScheduler}가 매시 정각에 flush한다.
 * 따라서 오늘 수치는 최대 1시간 늦다 — 화면에도 그 사실을 표기한다.
 */
@Service
@RequiredArgsConstructor
public class AdminAdStatService {

    /** 대시보드 기본 조회 구간(오늘 포함 N일). 직전 동일 길이 구간과 비교해 증감을 낸다. */
    public static final int DEFAULT_WINDOW_DAYS = 14;
    /** 직접 지정으로 받을 수 있는 최대 길이. 이보다 길면 막대가 의미를 잃고 쿼리도 무거워진다. */
    public static final int MAX_WINDOW_DAYS = 365;
    /** 이 일수를 넘으면 막대를 주 단위로 접는다(하루 막대가 실오라기처럼 얇아지는 것 방지). */
    private static final int WEEKLY_ROLLUP_THRESHOLD = 60;
    /** 화면의 기간 프리셋 버튼. 이 값으로 들어온 요청만 버튼이 선택된 상태로 표시된다. */
    public static final int[] PRESETS = {7, 14, 30, 90};

    private final BannerStatDailyRepository bannerStatDailyRepository;

    /** 배너 id → 전 기간 누적. 캠페인·광고주 합산은 이 값을 더해서 만든다. */
    @Transactional(readOnly = true)
    public Map<Long, Totals> totalsByBanner() {
        return indexByBannerId(bannerStatDailyRepository.sumGroupByBanner());
    }

    @Transactional(readOnly = true)
    public Map<Long, Totals> totalsByBannerBetween(LocalDate from, LocalDate to) {
        return indexByBannerId(bannerStatDailyRepository.sumGroupByBannerBetween(from, to));
    }

    /**
     * 화면이 넘긴 기간 파라미터를 실제로 조회할 구간으로 다듬는다.
     * 직접 지정(from·to)이 프리셋(days)보다 우선한다.
     *
     * 뒤집힌 기간·미래 날짜·과도하게 긴 구간은 <b>에러로 튕기지 않고 조용히 고친다</b> —
     * 어차피 없는 데이터를 요구한 것뿐이라 운영자를 막을 이유가 없다.
     */
    public Period normalizePeriod(Integer days, LocalDate from, LocalDate to, LocalDate today) {
        if (from == null || to == null) {
            int window = (days == null || days <= 0)
                    ? DEFAULT_WINDOW_DAYS
                    : Math.min(days, MAX_WINDOW_DAYS);
            return new Period(today.minusDays(window - 1L), today, isPreset(window) ? window : null);
        }

        LocalDate start = from;
        LocalDate end = to;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        if (end.isAfter(today)) {
            end = today;
        }
        if (start.isAfter(end)) {
            start = end;
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_WINDOW_DAYS) {
            start = end.minusDays(MAX_WINDOW_DAYS - 1L);
        }
        return new Period(start, end, null);
    }

    private boolean isPreset(int window) {
        for (int preset : PRESETS) {
            if (preset == window) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param from 조회 시작일(포함), @param to 조회 종료일(포함).
     *             정규화(오늘 이후 잘라내기·최대 길이 제한)는 {@link #normalizePeriod}가 끝낸 값을 받는다.
     */
    @Transactional(readOnly = true)
    public Dashboard getDashboard(LocalDate from, LocalDate to) {
        int days = (int) (ChronoUnit.DAYS.between(from, to) + 1);
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1L);

        List<DailyStatSum> current = bannerStatDailyRepository.sumGroupByDateBetween(from, to);
        List<DailyStatSum> previous = bannerStatDailyRepository.sumGroupByDateBetween(prevFrom, prevTo);

        List<DailyPoint> daily = toDailyPoints(current, from, to);
        long impressions = daily.stream().mapToLong(DailyPoint::getImpressions).sum();
        long clicks = daily.stream().mapToLong(DailyPoint::getClicks).sum();
        long prevImpressions = previous.stream().mapToLong(s -> nullSafe(s.getImpressions())).sum();
        long prevClicks = previous.stream().mapToLong(s -> nullSafe(s.getClicks())).sum();

        List<ChartBar> chart = toChartBars(daily);

        return new Dashboard(
                from, to, days,
                impressions, clicks, ctrOf(impressions, clicks),
                deltaPercent(impressions, prevImpressions),
                deltaPercent(clicks, prevClicks),
                prevImpressions == 0L ? null : ctrOf(impressions, clicks) - ctrOf(prevImpressions, prevClicks),
                prevFrom, prevTo,
                chart, chart.size() != daily.size());
    }

    /** 데이터가 없는 날도 0으로 채워야 차트 간격이 실제 날짜와 맞는다. */
    private List<DailyPoint> toDailyPoints(List<DailyStatSum> sums, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyStatSum> byDate = sums.stream()
                .collect(Collectors.toMap(DailyStatSum::getStatDate, Function.identity(), (a, b) -> a));

        long max = sums.stream().mapToLong(s -> nullSafe(s.getImpressions())).max().orElse(0L);

        List<DailyPoint> points = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailyStatSum sum = byDate.get(date);
            long impressions = sum != null ? nullSafe(sum.getImpressions()) : 0L;
            long clicks = sum != null ? nullSafe(sum.getClicks()) : 0L;
            points.add(new DailyPoint(date, impressions, clicks, heightPercent(impressions, max)));
        }
        return points;
    }

    /**
     * 막대 목록. 구간이 길면 하루 막대가 너무 얇아져 읽을 수 없으므로 주 단위로 접는다.
     * 접더라도 마지막 묶음은 7일이 안 될 수 있어(구간 끝이 딱 안 떨어짐) 라벨에 실제 시작·끝을 담는다.
     */
    private List<ChartBar> toChartBars(List<DailyPoint> daily) {
        if (daily.size() <= WEEKLY_ROLLUP_THRESHOLD) {
            long max = daily.stream().mapToLong(DailyPoint::getImpressions).max().orElse(0L);
            return daily.stream()
                    .map(p -> new ChartBar(p.getDate(), p.getDate(), p.getImpressions(), p.getClicks(),
                            heightPercent(p.getImpressions(), max)))
                    .toList();
        }

        List<ChartBar> buckets = new ArrayList<>();
        for (int i = 0; i < daily.size(); i += 7) {
            List<DailyPoint> week = daily.subList(i, Math.min(i + 7, daily.size()));
            buckets.add(new ChartBar(
                    week.get(0).getDate(),
                    week.get(week.size() - 1).getDate(),
                    week.stream().mapToLong(DailyPoint::getImpressions).sum(),
                    week.stream().mapToLong(DailyPoint::getClicks).sum(),
                    0));
        }
        long max = buckets.stream().mapToLong(ChartBar::getImpressions).max().orElse(0L);
        return buckets.stream()
                .map(b -> new ChartBar(b.getFrom(), b.getTo(), b.getImpressions(), b.getClicks(),
                        heightPercent(b.getImpressions(), max)))
                .toList();
    }

    /** 값이 있는 날은 최소 2%라도 그려야 "0과 아주 작은 값"이 구분된다. */
    private int heightPercent(long value, long max) {
        if (max <= 0L || value <= 0L) {
            return 0;
        }
        return (int) Math.max(2L, Math.round(value * 100.0 / max));
    }

    private Map<Long, Totals> indexByBannerId(List<BannerStatSum> sums) {
        Map<Long, Totals> map = new HashMap<>();
        for (BannerStatSum sum : sums) {
            map.put(sum.getBannerId(), new Totals(nullSafe(sum.getImpressions()), nullSafe(sum.getClicks())));
        }
        return map;
    }

    public static double ctrOf(long impressions, long clicks) {
        return impressions == 0L ? 0.0 : clicks * 100.0 / impressions;
    }

    /** 직전 구간이 0이면 증감률이 의미 없다(무한대) → null로 두고 화면에서 숨긴다. */
    private Double deltaPercent(long current, long previous) {
        if (previous <= 0L) {
            return null;
        }
        return (current - previous) * 100.0 / previous;
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    @Getter
    @AllArgsConstructor
    public static class Totals {
        private final long impressions;
        private final long clicks;

        public static Totals empty() {
            return new Totals(0L, 0L);
        }

        public Totals plus(Totals other) {
            return new Totals(impressions + other.impressions, clicks + other.clicks);
        }

        public double getCtr() {
            return ctrOf(impressions, clicks);
        }
    }

    /** @param preset 프리셋으로 정해진 경우 그 일수, 직접 지정이면 null(버튼 하이라이트 판단용). */
    public record Period(LocalDate from, LocalDate to, Integer preset) {

        public int days() {
            return (int) (ChronoUnit.DAYS.between(from, to) + 1);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DailyPoint {
        private final LocalDate date;
        private final long impressions;
        private final long clicks;
        private final int heightPercent;
    }

    @Getter
    @AllArgsConstructor
    public static class ChartBar {
        private final LocalDate from;
        private final LocalDate to;
        private final long impressions;
        private final long clicks;
        private final int heightPercent;

        public boolean isRange() {
            return !from.equals(to);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Dashboard {
        private final LocalDate from;
        private final LocalDate to;
        private final int days;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final Double impressionsDelta;
        private final Double clicksDelta;
        private final Double ctrDelta;
        private final LocalDate previousFrom;
        private final LocalDate previousTo;
        private final List<ChartBar> chart;
        /** 막대를 주 단위로 접었는지. 차트 제목을 바꾸는 데 쓴다. */
        private final boolean weekly;

        public boolean isEmpty() {
            return impressions == 0L && clicks == 0L;
        }

        /** 막대가 촘촘하면 간격을 좁혀야 한다(템플릿에서 클래스 분기). */
        public boolean isDense() {
            return chart.size() > 20;
        }
    }
}
