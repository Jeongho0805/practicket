package com.practicket.ad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 어드민 첫 화면. 선택한 기간의 노출·클릭·CTR과 추이, 상위 캠페인을 보여준다.
 *
 * 기간은 두 가지 방식으로 들어온다:
 * - {@code ?days=30} — 오늘 기준 최근 N일(프리셋 버튼)
 * - {@code ?from=2026-07-01&to=2026-07-26} — 직접 지정
 * 둘 다 없으면 기본 14일. 주소에 실리므로 새로고침·링크 공유에도 유지된다.
 */
@Controller
@RequestMapping("/admin-hoya/ad")
@RequiredArgsConstructor
public class AdminDashboardController {

    /** 프리셋 버튼에 노출하는 값. 템플릿이 이 배열로 버튼을 그린다. */
    private static final int[] PRESETS = {7, 14, 30, 90};

    private final AdminAdStatService adminAdStatService;

    @GetMapping
    public String dashboard(@RequestParam(required = false) Integer days,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model) {
        LocalDate today = LocalDate.now();
        Period period = resolvePeriod(days, from, to, today);

        model.addAttribute("dashboard", adminAdStatService.getDashboard(period.from(), period.to()));
        model.addAttribute("presets", PRESETS);
        model.addAttribute("activePreset", period.preset());
        return "admin/ad/dashboard";
    }

    /**
     * 직접 지정이 프리셋보다 우선한다. 뒤집힌 기간·미래 날짜·과도하게 긴 구간은
     * 에러로 튕기지 않고 조용히 다듬는다 — 어차피 없는 데이터를 요구한 것뿐이라
     * 운영자를 막을 이유가 없다.
     */
    private Period resolvePeriod(Integer days, LocalDate from, LocalDate to, LocalDate today) {
        if (from == null || to == null) {
            int window = (days == null || days <= 0)
                    ? AdminAdStatService.DEFAULT_WINDOW_DAYS
                    : Math.min(days, AdminAdStatService.MAX_WINDOW_DAYS);
            Integer preset = isPreset(window) ? window : null;
            return new Period(today.minusDays(window - 1L), today, preset);
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
        if (ChronoUnit.DAYS.between(start, end) + 1 > AdminAdStatService.MAX_WINDOW_DAYS) {
            start = end.minusDays(AdminAdStatService.MAX_WINDOW_DAYS - 1L);
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

    /** @param preset 프리셋으로 정해진 경우 그 일수, 직접 지정이면 null(버튼 하이라이트 판단용). */
    private record Period(LocalDate from, LocalDate to, Integer preset) {
    }
}
