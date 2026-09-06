package com.practicket.ad.application;

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

/** 거래처 CRUD. 캠페인 폼이 여기서 만든 목록에서 광고주를 고른다. */
@Controller
@RequestMapping("/admin-hoya/ad/advertisers")
@RequiredArgsConstructor
public class AdminAdvertiserController {

    private final AdminAdvertiserService adminAdvertiserService;

    @InitBinder
    void trimEmptyToNull(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", adminAdvertiserService.getRows());
        return "admin/ad/advertiser-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AdminAdvertiserService.AdvertiserForm());
        return "admin/ad/advertiser-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        return adminAdvertiserService.findDetail(id)
                .map(detail -> {
                    model.addAttribute("detail", detail);
                    return "admin/ad/advertiser-detail";
                })
                .orElse("redirect:/admin-hoya/ad/advertisers");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return adminAdvertiserService.findForm(id)
                .map(form -> {
                    model.addAttribute("form", form);
                    return "admin/ad/advertiser-form";
                })
                .orElse("redirect:/admin-hoya/ad/advertisers");
    }

    @PostMapping
    public String save(@ModelAttribute AdminAdvertiserService.AdvertiserForm form,
                       RedirectAttributes redirectAttributes) {
        try {
            Long id = adminAdvertiserService.save(form);
            return "redirect:/admin-hoya/ad/advertisers/" + id;
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return form.getId() == null
                    ? "redirect:/admin-hoya/ad/advertisers/new"
                    : "redirect:/admin-hoya/ad/advertisers/" + form.getId() + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminAdvertiserService.delete(id);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/advertisers/" + id;
        }
        return "redirect:/admin-hoya/ad/advertisers";
    }
}
