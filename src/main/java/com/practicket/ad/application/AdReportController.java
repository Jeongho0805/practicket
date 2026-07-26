package com.practicket.ad.application;

import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.domain.BannerStatDaily;
import com.practicket.ad.domain.BannerStatDailyRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 광고주가 로그인 없이 보는 토큰 기반 캠페인 리포트 페이지.
 * docs/ad-admin-system.md Q5: /ad/report/{token}, 총 노출/클릭/CTR + 일별 추이.
 */
@Controller
@RequiredArgsConstructor
public class AdReportController {

    private final BannerRepository bannerRepository;
    private final BannerStatDailyRepository bannerStatDailyRepository;

    @GetMapping("/ad/report/{token}")
    public String report(@PathVariable String token, Model model) {
        Banner banner = bannerRepository.findByReportToken(token)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate periodTo = (banner.getEndAt() != null && banner.getEndAt().isBefore(today))
                ? banner.getEndAt() : today;
        LocalDate periodFrom = banner.getStartAt() != null ? banner.getStartAt() : periodTo;
        if (periodFrom.isAfter(periodTo)) {
            periodFrom = periodTo;
        }

        List<BannerStatDaily> stats = bannerStatDailyRepository
                .findByBannerIdAndStatDateBetweenOrderByStatDate(banner.getId(), periodFrom, periodTo);
        Map<LocalDate, BannerStatDaily> statsByDate = stats.stream()
                .collect(Collectors.toMap(BannerStatDaily::getStatDate, s -> s));

        List<DailyRow> dailyRows = new ArrayList<>();
        long totalImpressions = 0L;
        long totalClicks = 0L;
        for (LocalDate date = periodFrom; !date.isAfter(periodTo); date = date.plusDays(1)) {
            BannerStatDaily stat = statsByDate.get(date);
            long impressions = stat != null ? stat.getImpressions() : 0L;
            long clicks = stat != null ? stat.getClicks() : 0L;
            totalImpressions += impressions;
            totalClicks += clicks;
            dailyRows.add(new DailyRow(date, impressions, clicks));
        }

        long maxImpressions = dailyRows.stream().mapToLong(DailyRow::getImpressions).max().orElse(0L);
        double ctr = totalImpressions == 0L ? 0.0 : (totalClicks * 100.0 / totalImpressions);

        model.addAttribute("banner", banner);
        model.addAttribute("periodFrom", periodFrom);
        model.addAttribute("periodTo", periodTo);
        model.addAttribute("totalImpressions", totalImpressions);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("ctr", ctr);
        model.addAttribute("dailyRows", dailyRows);
        model.addAttribute("maxImpressions", Math.max(maxImpressions, 1L));

        return "ad/report";
    }

    @Getter
    @AllArgsConstructor
    private static class DailyRow {
        private final LocalDate date;
        private final long impressions;
        private final long clicks;
    }
}
