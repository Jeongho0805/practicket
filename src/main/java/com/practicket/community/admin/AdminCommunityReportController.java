package com.practicket.community.admin;

import com.practicket.community.admin.dto.BanDuration;
import com.practicket.community.admin.dto.ReportedTargetRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 신고가 1건이라도 들어오면 뜬다 — 자동 블라인드 임계치는 노출 기준이 아니다.
 * 처리(블라인드·삭제·밴)는 {@link AdminCommunityModerationController} 가 맡는다.
 */
@Controller
@RequestMapping("/admin-hoya/community/reports")
@RequiredArgsConstructor
public class AdminCommunityReportController {

    private static final int PAGE_SIZE = 20;

    private final AdminCommunityService adminCommunityService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<ReportedTargetRow> reports = adminCommunityService.getReportedTargets(PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("reports", reports);
        model.addAttribute("durations", BanDuration.values());
        return "admin/community/report-list";
    }
}
