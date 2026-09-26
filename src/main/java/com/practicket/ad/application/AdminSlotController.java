package com.practicket.ad.application;

import com.practicket.ad.component.AdNetworkSettings;
import com.practicket.ad.component.AdSlotSnapshotStore;
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

import java.util.List;

/** 자리 목록과 값 수정. 자리 신설·삭제는 없다 — 템플릿에 조각이 있어야 실제로 뜬다. */
@Controller
@RequestMapping("/admin-hoya/ad/slots")
@RequiredArgsConstructor
public class AdminSlotController {

    private static final List<String> NETWORKS = List.of(
            AdNetworkSettings.COUPANG, AdNetworkSettings.ADSENSE, AdNetworkSettings.ADFIT,
            AdNetworkSettings.MOBSENSE);

    private final AdminSlotService adminSlotService;
    private final AdNetworkExposureService adNetworkExposureService;
    private final AdSlotSnapshotStore adSlotSnapshotStore;

    @InitBinder
    void trimEmptyToNull(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", adminSlotService.getGroups());
        model.addAttribute("units", adminSlotService.allUnits());
        model.addAttribute("networks", NETWORKS);
        model.addAttribute("networkLabels", AdNetworkSettings.LABELS);
        model.addAttribute("refillGaps", adNetworkExposureService.currentRefillGaps());
        return "admin/ad/slot-list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return adminSlotService.findForm(id)
                .map(form -> {
                    model.addAttribute("form", form);
                    model.addAttribute("networks", NETWORKS);
                    model.addAttribute("units", adminSlotService.unitOptions(form));
                    model.addAttribute("networkLabels", AdNetworkSettings.LABELS);
                    model.addAttribute("unitWarning", adminSlotService.oversizeWarning(form));
                    return "admin/ad/slot-form";
                })
                .orElse("redirect:/admin-hoya/ad/slots");
    }

    @PostMapping
    public String save(@ModelAttribute AdminSlotService.SlotForm form, RedirectAttributes redirectAttributes) {
        try {
            adminSlotService.save(form);
            adSlotSnapshotStore.refresh();
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/slots/" + form.getId() + "/edit";
        }
        return "redirect:/admin-hoya/ad/slots";
    }

    /** 목록의 채움 순서. index 는 0부터이고 0 이 1단계다 */
    @PostMapping("/{id}/steps")
    public String addStep(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return apply(() -> adminSlotService.addStep(id), redirectAttributes);
    }

    @PostMapping("/{id}/steps/{index}")
    public String changeStep(@PathVariable Long id, @PathVariable int index,
                             @RequestParam(required = false) String network,
                             @RequestParam(required = false) Long pcAdUnitId,
                             @RequestParam(required = false) Long mobileAdUnitId,
                             RedirectAttributes redirectAttributes) {
        return apply(() -> adminSlotService.changeStep(id, index, network, pcAdUnitId, mobileAdUnitId),
                redirectAttributes);
    }

    @PostMapping("/{id}/steps/{index}/move")
    public String moveStep(@PathVariable Long id, @PathVariable int index, @RequestParam int to,
                           RedirectAttributes redirectAttributes) {
        return apply(() -> adminSlotService.moveStep(id, index, to), redirectAttributes);
    }

    @PostMapping("/{id}/steps/{index}/delete")
    public String removeStep(@PathVariable Long id, @PathVariable int index, RedirectAttributes redirectAttributes) {
        return apply(() -> adminSlotService.removeStep(id, index), redirectAttributes);
    }

    private String apply(Runnable change, RedirectAttributes redirectAttributes) {
        try {
            change.run();
            adSlotSnapshotStore.refresh();
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/ad/slots";
    }
}
