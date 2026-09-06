package com.practicket.ad.application;

import com.practicket.ad.component.AdNetworkSettings;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 자리 목록과 값 수정. 자리 신설·삭제는 없다 — 템플릿에 조각이 있어야 실제로 뜬다. */
@Controller
@RequestMapping("/admin-hoya/ad/slots")
@RequiredArgsConstructor
public class AdminSlotController {

    private final AdminSlotService adminSlotService;

    @InitBinder
    void trimEmptyToNull(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", adminSlotService.getGroups());
        return "admin/ad/slot-list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return adminSlotService.findForm(id)
                .map(form -> {
                    model.addAttribute("form", form);
                    model.addAttribute("networks", List.of(
                            AdNetworkSettings.COUPANG, AdNetworkSettings.ADSENSE, AdNetworkSettings.ADFIT));
                    model.addAttribute("units", adminSlotService.unitOptions(form));
                    model.addAttribute("unitWarning", adminSlotService.oversizeWarning(form));
                    return "admin/ad/slot-form";
                })
                .orElse("redirect:/admin-hoya/ad/slots");
    }

    @PostMapping
    public String save(@ModelAttribute AdminSlotService.SlotForm form, RedirectAttributes redirectAttributes) {
        try {
            adminSlotService.save(form);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/slots/" + form.getId() + "/edit";
        }
        return "redirect:/admin-hoya/ad/slots";
    }
}
