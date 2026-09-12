package com.practicket.ad.application;

import com.practicket.ad.component.AdSlotSnapshotStore;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.CampaignStatus;
import com.practicket.ad.exception.AdException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 캠페인(계약) 화면. 배너는 독립 메뉴가 아니라 이 폼 안에서만 만들어지고 지워진다 —
 * 배너 하나만 따로 만들면 어느 계약에 속하는지 알 수 없어 리포트도 정산도 붙지 않는다.
 *
 * 빈 문자열을 null 로 바꾸는 편집기를 다는 이유는 배너의 기간 칸 때문이다. 비워 두면
 * "계약 기간을 따른다"는 뜻인데, 그대로 두면 빈 문자열이 날짜 변환에서 터진다.
 */
@Controller
@RequestMapping("/admin-hoya/ad/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final AdminCampaignService adminCampaignService;
    private final AdSlotSnapshotStore adSlotSnapshotStore;
    private final AdvertiserRepository advertiserRepository;
    private final AdSlotRepository adSlotRepository;

    @InitBinder
    void trimEmptyToNull(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "all") String status,
                       @RequestParam(required = false) String q,
                       Model model) {
        List<AdminCampaignService.CampaignRow> all = adminCampaignService.getCampaignRows();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", (long) all.size());
        for (CampaignStatus value : CampaignStatus.values()) {
            counts.put(value.getKey(), all.stream().filter(row -> row.getStatus() == value).count());
        }

        String keyword = q == null ? "" : q.trim().toLowerCase();
        model.addAttribute("rows", all.stream()
                .filter(row -> "all".equals(status) || row.getStatus().getKey().equals(status))
                .filter(row -> keyword.isEmpty()
                        || row.getName().toLowerCase().contains(keyword)
                        || row.getAdvertiserName().toLowerCase().contains(keyword))
                .toList());
        model.addAttribute("counts", counts);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        return "admin/ad/campaign-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", adminCampaignService.blankForm());
        addFormOptions(model);
        return "admin/ad/campaign-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        return adminCampaignService.findDetail(id)
                .map(detail -> {
                    model.addAttribute("detail", detail);
                    return "admin/ad/campaign-detail";
                })
                .orElse("redirect:/admin-hoya/ad/campaigns");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return adminCampaignService.findForm(id)
                .map(form -> {
                    model.addAttribute("form", form);
                    addFormOptions(model);
                    return "admin/ad/campaign-form";
                })
                .orElse("redirect:/admin-hoya/ad/campaigns");
    }

    /** 검증에 걸리면 되돌리지 않고 받은 폼을 그대로 다시 그린다. 파일만 다시 골라야 한다 */
    @PostMapping
    public String save(@ModelAttribute("form") AdminCampaignService.CampaignForm form, Model model) {
        try {
            Long id = adminCampaignService.save(form);
            adSlotSnapshotStore.refresh();
            return "redirect:/admin-hoya/ad/campaigns/" + id;
        } catch (AdException e) {
            model.addAttribute("error", e.getMessage() + " 올렸던 파일은 다시 골라주세요.");
            addFormOptions(model);
            return "admin/ad/campaign-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminCampaignService.delete(id);
            adSlotSnapshotStore.refresh();
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/campaigns/" + id;
        }
        return "redirect:/admin-hoya/ad/campaigns";
    }

    @PostMapping("/{campaignId}/banners/{bannerId}/toggle")
    public String toggleBanner(@PathVariable Long campaignId, @PathVariable Long bannerId,
                               RedirectAttributes redirectAttributes) {
        try {
            adminCampaignService.toggleBanner(bannerId);
            adSlotSnapshotStore.refresh();
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/ad/campaigns/" + campaignId;
    }

    private void addFormOptions(Model model) {
        model.addAttribute("advertisers", advertiserRepository.findAllByOrderByNameAsc());
        model.addAttribute("slots", adSlotRepository.findAll());
    }
}
