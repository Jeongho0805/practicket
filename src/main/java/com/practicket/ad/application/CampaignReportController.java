package com.practicket.ad.application;

import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdCampaignRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.domain.BannerStatus;
import com.practicket.ad.domain.BannerStatDailyRepository;
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
 * 광고주가 로그인 없이 보는 계약 리포트. 자리를 셋 산 광고주가 링크를 셋 받던 것을 하나로 합쳤고,
 * 수치는 그 계약에 달린 배너들을 합산한다.
 *
 * 토큰은 계약 행에 저장돼 있다. 이전의 이름 서명 방식과 달리 계약 하나만 따로 폐기할 수 있다.
 */
@Controller
@RequiredArgsConstructor
public class CampaignReportController {

    private final AdCampaignRepository adCampaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final BannerRepository bannerRepository;
    private final BannerStatDailyRepository bannerStatDailyRepository;

    @GetMapping("/ad/report/campaign/{token}")
    public String report(@PathVariable String token, Model model) {
        AdCampaign campaign = adCampaignRepository.findByReportToken(token)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        List<Banner> banners = bannerRepository.findByCampaignIdWithSlot(campaign.getId()).stream()
                .sorted(Comparator.comparing(Banner::getId))
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate periodFrom = campaign.getStartAt();
        LocalDate periodTo = campaign.getEndAt().isAfter(today) ? today : campaign.getEndAt();
        if (periodFrom.isAfter(periodTo)) {
            periodFrom = periodTo;   // 아직 시작 전인 계약. 표는 비지만 화면은 정상적으로 뜬다
        }

        Map<LocalDate, DailyStatSum> statsByDate = bannerStatDailyRepository
                .sumGroupByDateForBanners(banners.stream().map(Banner::getId).toList(), periodFrom, periodTo)
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

        model.addAttribute("campaign", campaign);
        model.addAttribute("advertiserName", advertiserRepository.findById(campaign.getAdvertiserId())
                .map(Advertiser::getName).orElse(campaign.getName()));
        model.addAttribute("slots", toSlotRows(campaign, banners, today));
        model.addAttribute("periodFrom", periodFrom);
        model.addAttribute("periodTo", periodTo);
        model.addAttribute("totalImpressions", totalImpressions);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("ctr", totalImpressions == 0L ? 0.0 : totalClicks * 100.0 / totalImpressions);
        model.addAttribute("dailyRows", dailyRows);
        model.addAttribute("maxImpressions",
                Math.max(dailyRows.stream().mapToLong(DailyRow::getImpressions).max().orElse(0L), 1L));
        return "ad/campaign-report";
    }

    private List<SlotRow> toSlotRows(AdCampaign campaign, List<Banner> banners, LocalDate today) {
        Map<Long, com.practicket.ad.domain.BannerStatSum> totals =
                bannerStatDailyRepository.sumGroupByBanner().stream()
                        .collect(Collectors.toMap(com.practicket.ad.domain.BannerStatSum::getBannerId,
                                Function.identity(), (a, b) -> a));

        return banners.stream()
                .map(banner -> {
                    var sum = totals.get(banner.getId());
                    long impressions = sum != null ? nullSafe(sum.getImpressions()) : 0L;
                    long clicks = sum != null ? nullSafe(sum.getClicks()) : 0L;
                    return new SlotRow(
                            banner.getSlot().getName(),
                            BannerStatus.effectiveStart(banner, campaign),
                            BannerStatus.effectiveEnd(banner, campaign),
                            impressions, clicks,
                            impressions == 0L ? 0.0 : clicks * 100.0 / impressions,
                            BannerStatus.of(banner, campaign, today).getLabel());
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
    public static class SlotRow {
        private final String slotName;
        private final LocalDate startAt;
        private final LocalDate endAt;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final String statusLabel;
    }
}
