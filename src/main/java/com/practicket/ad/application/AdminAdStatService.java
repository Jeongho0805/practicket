package com.practicket.ad.application;

import com.practicket.ad.component.AdvertiserReportToken;
import com.practicket.ad.domain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 어드민 화면이 쓰는 조회 전용 집계. banner_stat_daily는 배너×날짜 롤업이라
 * 화면에 필요한 합산/추이는 여기서 만들어 뷰 모델로 넘긴다.
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
    private static final int TOP_CAMPAIGNS = 5;
    /** 화면의 기간 프리셋 버튼. 이 값으로 들어온 요청만 버튼이 선택된 상태로 표시된다. */
    public static final int[] PRESETS = {7, 14, 30, 90};

    private final BannerRepository bannerRepository;
    private final AdSlotRepository adSlotRepository;
    private final BannerStatDailyRepository bannerStatDailyRepository;
    private final AdvertiserReportToken advertiserReportToken;

    /** 배너 목록 화면 — 배너 + 전 기간 누적 성과 + 상태. */
    @Transactional(readOnly = true)
    public List<BannerRow> getBannerRows() {
        LocalDate today = LocalDate.now();
        Map<Long, BannerStatSum> totals = indexByBannerId(bannerStatDailyRepository.sumGroupByBanner());

        return bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .map(banner -> toRow(banner, totals.get(banner.getId()), today))
                .toList();
    }

    /**
     * 광고주 목록 — 배너의 advertiserName이 같은 것끼리 묶는다.
     * 별도 advertiser 테이블을 두지 않은 선택이라 이름이 곧 식별자다(등록 폼의 자동완성으로 오타를 줄인다).
     */
    @Transactional(readOnly = true)
    public List<AdvertiserRow> getAdvertiserRows() {
        Map<String, List<BannerRow>> grouped = new LinkedHashMap<>();
        for (BannerRow row : getBannerRows()) {
            grouped.computeIfAbsent(displayName(row.getBanner().getAdvertiserName()), k -> new ArrayList<>()).add(row);
        }
        return grouped.entrySet().stream()
                .map(entry -> toAdvertiserRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(AdvertiserRow::getImpressions).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AdvertiserRow> findAdvertiser(String name) {
        return getAdvertiserRows().stream()
                .filter(row -> row.getName().equals(name))
                .findFirst();
    }

    /**
     * 배너 등록/수정 폼에서 "이 슬롯에 이미 배너가 있다"고 알려주기 위한 자료.
     * 같은 슬롯에 기간이 겹치는 배너가 둘 이상이면 {@code AdRenderService}가 분 단위로 로테이션하므로
     * 각자의 노출이 나뉜다 — 단독 노출로 판 자리라면 사고다. 막지는 않고 경고만 한다.
     *
     * 노출이 꺼진 배너는 어차피 렌더되지 않으니 제외한다. 날짜는 JS에서 그대로 비교하도록 ISO 문자열로 넘긴다.
     */
    @Transactional(readOnly = true)
    public List<SlotOccupancy> getSlotOccupancies() {
        return bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .filter(banner -> Boolean.TRUE.equals(banner.getEnabled()))
                .filter(banner -> banner.getStartAt() != null && banner.getEndAt() != null)
                .map(banner -> new SlotOccupancy(
                        banner.getId(),
                        banner.getSlot().getId(),
                        displayName(banner.getAdvertiserName()),
                        banner.getStartAt().toString(),
                        banner.getEndAt().toString()))
                .toList();
    }

    /** 배너 등록 폼의 광고주명 자동완성 목록. 이름 표기가 갈라지는 걸 막는 유일한 장치다. */
    @Transactional(readOnly = true)
    public List<String> getAdvertiserNames() {
        return bannerRepository.findAll().stream()
                .map(Banner::getAdvertiserName)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private AdvertiserRow toAdvertiserRow(String name, List<BannerRow> banners) {
        long impressions = banners.stream().mapToLong(BannerRow::getImpressions).sum();
        long clicks = banners.stream().mapToLong(BannerRow::getClicks).sum();
        return new AdvertiserRow(
                name,
                banners.size(),
                banners.stream().filter(b -> b.getStatus() == BannerStatus.LIVE).count(),
                impressions, clicks, ctrOf(impressions, clicks),
                banners.stream().map(b -> b.getBanner().getStartAt())
                        .filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null),
                banners.stream().map(b -> b.getBanner().getEndAt())
                        .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null),
                banners.stream()
                        .sorted(Comparator.comparing(
                                (BannerRow b) -> b.getBanner().getStartAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList(),
                "/ad/report/advertiser/" + advertiserReportToken.issue(name));
    }

    private String displayName(String advertiserName) {
        return (advertiserName == null || advertiserName.isBlank()) ? "(광고주명 없음)" : advertiserName.trim();
    }

    /** 슬롯 목록 화면 — 슬롯 + 지금 그 슬롯에 실제로 걸려 있는 배너 수. */
    @Transactional(readOnly = true)
    public List<SlotRow> getSlotRows() {
        LocalDate today = LocalDate.now();
        Map<Long, Long> liveCounts = countLiveBannersBySlot(today);

        return adSlotRepository.findAll().stream()
                .map(slot -> new SlotRow(slot, liveCounts.getOrDefault(slot.getId(), 0L)))
                .toList();
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
        LocalDate today = LocalDate.now();

        return new Dashboard(
                from, to, days,
                impressions, clicks, ctrOf(impressions, clicks),
                deltaPercent(impressions, prevImpressions),
                deltaPercent(clicks, prevClicks),
                prevImpressions == 0L ? null : ctrOf(impressions, clicks) - ctrOf(prevImpressions, prevClicks),
                prevFrom, prevTo,
                chart, chart.size() != daily.size(),
                topCampaigns(from, to),
                bannerSummary(today),
                slotSummary(today));
    }

    private List<BannerRow> topCampaigns(LocalDate from, LocalDate to) {
        Map<Long, BannerStatSum> periodTotals =
                indexByBannerId(bannerStatDailyRepository.sumGroupByBannerBetween(from, to));

        return bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .map(banner -> toRow(banner, periodTotals.get(banner.getId()), to))
                .sorted(Comparator.comparingLong(BannerRow::getImpressions).reversed())
                .limit(TOP_CAMPAIGNS)
                .toList();
    }

    private BannerSummary bannerSummary(LocalDate today) {
        List<Banner> banners = bannerRepository.findAll();
        long live = banners.stream().filter(b -> BannerStatus.of(b, today) == BannerStatus.LIVE).count();
        return new BannerSummary(banners.size(), live);
    }

    private SlotSummary slotSummary(LocalDate today) {
        Map<Long, Long> liveCounts = countLiveBannersBySlot(today);
        List<AdSlot> enabled = adSlotRepository.findAll().stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getEnabled()))
                .toList();
        long sold = enabled.stream().filter(slot -> liveCounts.getOrDefault(slot.getId(), 0L) > 0L).count();
        return new SlotSummary(enabled.size(), sold);
    }

    private Map<Long, Long> countLiveBannersBySlot(LocalDate today) {
        return bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .filter(banner -> BannerStatus.of(banner, today) == BannerStatus.LIVE)
                .collect(Collectors.groupingBy(banner -> banner.getSlot().getId(), Collectors.counting()));
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

    private BannerRow toRow(Banner banner, BannerStatSum sum, LocalDate today) {
        long impressions = sum != null ? nullSafe(sum.getImpressions()) : 0L;
        long clicks = sum != null ? nullSafe(sum.getClicks()) : 0L;
        return new BannerRow(banner, impressions, clicks, ctrOf(impressions, clicks),
                BannerStatus.of(banner, today));
    }

    private Map<Long, BannerStatSum> indexByBannerId(List<BannerStatSum> sums) {
        Map<Long, BannerStatSum> map = new HashMap<>();
        for (BannerStatSum sum : sums) {
            map.put(sum.getBannerId(), sum);
        }
        return map;
    }

    private double ctrOf(long impressions, long clicks) {
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
    public static class BannerRow {
        private final Banner banner;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final BannerStatus status;

        public String getReportPath() {
            return banner.getReportToken() == null ? null : "/ad/report/" + banner.getReportToken();
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
    public static class SlotOccupancy {
        private final Long bannerId;
        private final Long slotId;
        private final String advertiserName;
        private final String startAt;
        private final String endAt;
    }

    @Getter
    @AllArgsConstructor
    public static class AdvertiserRow {
        private final String name;
        private final int campaigns;
        private final long liveCampaigns;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final LocalDate firstStart;
        private final LocalDate lastEnd;
        /** 최근 시작 순. 상세 화면이 그대로 쓴다. */
        private final List<BannerRow> banners;
        /** 광고주에게 통째로 건네는 통합 리포트 주소. 이름을 서명해 만든 값이라 저장하지 않는다. */
        private final String reportPath;
    }

    @Getter
    @AllArgsConstructor
    public static class SlotRow {
        private final AdSlot slot;
        private final long liveBannerCount;

        /** 슬롯 카드의 채움 게이지. 한 슬롯에 여러 배너가 걸릴 수 있어 상한을 100%로 자른다. */
        public int getFillPercent() {
            return (int) Math.min(100L, liveBannerCount * 100L);
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
    public static class BannerSummary {
        private final int total;
        private final long live;
    }

    @Getter
    @AllArgsConstructor
    public static class SlotSummary {
        private final int enabled;
        private final long sold;
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
        private final List<BannerRow> topCampaigns;
        private final BannerSummary banners;
        private final SlotSummary slots;

        public boolean isEmpty() {
            return impressions == 0L && clicks == 0L;
        }

        /** 막대가 촘촘하면 간격을 좁혀야 한다(템플릿에서 클래스 분기). */
        public boolean isDense() {
            return chart.size() > 20;
        }
    }
}
