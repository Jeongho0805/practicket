package com.practicket.notice.application;

import com.practicket.notice.domain.NoticeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 어드민 공지 CRUD. 화면은 admin/ad/* 와 같은 standalone 셸(admin/fragments/admin-shell)을 재사용한다.
 */
@Slf4j
@Controller
@RequestMapping("/admin-hoya/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notices", adminNoticeService.getAll());
        return "admin/notice/notice-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("notice", null);
        model.addAttribute("types", NoticeType.values());
        return "admin/notice/notice-form";
    }

    @PostMapping
    public String create(
            @RequestParam NoticeType type,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "false") boolean pinned,
            @RequestParam(required = false, defaultValue = "false") boolean published,
            RedirectAttributes redirectAttributes) {
        try {
            adminNoticeService.create(type, title, content, pinned, published);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/notices/new";
        }
        return "redirect:/admin-hoya/notices";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("notice", adminNoticeService.get(id));
        } catch (IllegalArgumentException e) {
            log.warn("공지 수정 화면 진입 실패: id={}, message={}", id, e.getMessage());
            return "redirect:/admin-hoya/notices";
        }
        model.addAttribute("types", NoticeType.values());
        return "admin/notice/notice-form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam NoticeType type,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "false") boolean pinned,
            @RequestParam(required = false, defaultValue = "false") boolean published,
            RedirectAttributes redirectAttributes) {
        try {
            adminNoticeService.update(id, type, title, content, pinned, published);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/notices/" + id + "/edit";
        }
        return "redirect:/admin-hoya/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminNoticeService.delete(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/notices";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminNoticeService.togglePublished(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/notices";
    }
}
