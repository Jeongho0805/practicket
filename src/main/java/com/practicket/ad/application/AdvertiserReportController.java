package com.practicket.ad.application;

import com.practicket.ad.component.AdvertiserReportToken;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.domain.BannerStatDailyRepository;
import com.practicket.ad.domain.BannerStatus;
import com.practicket.ad.domain.DailyStatSum;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 광고주가 로그인 없이 보는 통합 리포트. 그 광고주의 모든 캠페인을 합쳐 보여준다.
 * 캠페인 하나짜리 리포트는 기존 {@link AdReportController}(/ad/report/{token})가 그대로 담당한다.
 *
 * 광고주는 테이블이 아니라 배너의 이름 문자열이므로, 토큰을 저장하는 대신
 * {@link AdvertiserReportToken}이 이름을 서명한 값과 대조해 찾는다.
 */
@Controller
@RequiredArgsConstructor
public class AdvertiserReportController {

    private final BannerRepository bannerRepository;
    private final BannerStatDailyRepository bannerStatDailyRepository;
    private final AdvertiserReportToken advertiserReportToken;

    @GetMapping("/ad/report/advertiser/{token}")
    public String report(@PathVariable String token, Model model) {
        List<Banner> banners = bannerRepository.findAllWithSlotOrderByCreatedAtDesc();

        String advertiserName = banners.stream()
                .map(Banner::getAdvertiserName)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .filter(name -> advertiserReportToken.matches(name, token))
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        List<Banner> mine = banners.stream()
                .filter(banner -> advertiserName.equals(
                        banner.getAdvertiserName() == null ? null : banner.getAdvertiserName().trim()))
                .sorted(Comparator.comparing(Banner::getStartAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate periodFrom = mine.stream().map(Banner::getStartAt)
                .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(today);
        LocalDate periodTo = mine.stream().map(Banner::getEndAt)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(today);
        if (periodTo.isAfter(today)) {
            periodTo = today;   // 아직 오지 않은 날까지 표를 그릴 이유가 없다
        }
        if (periodFrom.isAfter(periodTo)) {
            periodFrom = periodTo;
        }

        Map<LocalDate, DailyStatSum> statsByDate = bannerStatDailyRepository
                .sumGroupByDateForBanners(mine.stream().map(Banner::getId).toList(), periodFrom, periodTo)
                .stream()
                .collect(Collectors.toMap(DailyStatSum::getStatDate, Function.identity(), (a, b) -> a));

        List<DailyRow> dailyRows = new ArrayList<>();
        long totalImpressions = 0L;
        long totalClicks = 0L;
        for (LocalDate date = periodFrom; !date.isAfter(periodTo); date = date.plusDays(1)) {
            DailyStatSum stat = statsByDate.get(date);
            long impressions = stat != null ? nullSafe(stat.getImpressions()) : 0L;
            long clicks = stat != null ? nullSafe(stat.getClicks()) : 0L;
            totalImpressions += impressions;
            totalClicks += clicks;
            dailyRows.add(new DailyRow(date, impressions, clicks));
        }

        model.addAttribute("advertiserName", advertiserName);
        model.addAttribute("campaigns", toCampaignRows(mine, today));
        model.addAttribute("periodFrom", periodFrom);
        model.addAttribute("periodTo", periodTo);
        model.addAttribute("totalImpressions", totalImpressions);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("ctr", totalImpressions == 0L ? 0.0 : totalClicks * 100.0 / totalImpressions);
        model.addAttribute("dailyRows", dailyRows);
        model.addAttribute("maxImpressions",
                Math.max(dailyRows.stream().mapToLong(DailyRow::getImpressions).max().orElse(0L), 1L));
        return "ad/advertiser-report";
    }

    private List<CampaignRow> toCampaignRows(List<Banner> banners, LocalDate today) {
        Map<Long, com.practicket.ad.domain.BannerStatSum> totals =
                bannerStatDailyRepository.sumGroupByBanner().stream()
                        .collect(Collectors.toMap(com.practicket.ad.domain.BannerStatSum::getBannerId,
                                Function.identity(), (a, b) -> a));

        return banners.stream()
                .map(banner -> {
                    var sum = totals.get(banner.getId());
                    long impressions = sum != null ? nullSafe(sum.getImpressions()) : 0L;
                    long clicks = sum != null ? nullSafe(sum.getClicks()) : 0L;
                    return new CampaignRow(banner, impressions, clicks,
                            impressions == 0L ? 0.0 : clicks * 100.0 / impressions,
                            BannerStatus.of(banner, today).getLabel());
                })
                .toList();
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    @Getter
    @AllArgsConstructor
    public static class DailyRow {
        private final LocalDate date;
        private final long impressions;
        private final long clicks;
    }

    @Getter
    @AllArgsConstructor
    public static class CampaignRow {
        private final Banner banner;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final String statusLabel;
    }
}
