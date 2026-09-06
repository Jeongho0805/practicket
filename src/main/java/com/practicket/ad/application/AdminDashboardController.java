package com.practicket.ad.application;

import com.practicket.ad.domain.CampaignStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * 어드민 첫 화면. 선택한 기간의 노출·클릭·CTR과 추이, 진행 중인 계약을 보여준다.
 *
 * 기간은 두 가지 방식으로 들어온다:
 * - {@code ?days=30} — 오늘 기준 최근 N일(프리셋 버튼)
 * - {@code ?from=2026-07-01&to=2026-07-26} — 직접 지정
 * 둘 다 없으면 기본 14일. 주소에 실리므로 새로고침·링크 공유에도 유지된다.
 * 값을 다듬는 규칙은 {@link AdminAdStatService#normalizePeriod}가 갖는다.
 */
@Controller
@RequestMapping("/admin-hoya/ad")
@RequiredArgsConstructor
public class AdminDashboardController {

    private static final int TOP_CAMPAIGNS = 5;

    private final AdminAdStatService adminAdStatService;
    private final AdminCampaignService adminCampaignService;
    private final AdminSlotService adminSlotService;

    @GetMapping
    public String dashboard(@RequestParam(required = false) Integer days,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model) {
        AdminAdStatService.Period period =
                adminAdStatService.normalizePeriod(days, from, to, LocalDate.now());

        List<AdminCampaignService.CampaignRow> campaigns = adminCampaignService.getCampaignRows();
        List<AdminCampaignService.CampaignRow> live = campaigns.stream()
                .filter(row -> row.getStatus() == CampaignStatus.LIVE)
                .toList();

        model.addAttribute("dashboard", adminAdStatService.getDashboard(period.from(), period.to()));
        model.addAttribute("presets", AdminAdStatService.PRESETS);
        model.addAttribute("activePreset", period.preset());
        model.addAttribute("campaigns", campaigns.stream().limit(TOP_CAMPAIGNS).toList());
        model.addAttribute("liveCount", live.size());
        model.addAttribute("liveAmount", live.stream().mapToLong(AdminCampaignService.CampaignRow::getAmount).sum());
        model.addAttribute("unitGaps", adminSlotService.getGroups().stream()
                .flatMap(group -> group.getSlots().stream())
                .filter(AdminSlotService.SlotRow::isUnitMissing)
                .toList());
        return "admin/ad/dashboard";
    }
}
