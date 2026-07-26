package com.practicket.ad.application;

import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.exception.AdException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * 어드민 배너 CRUD. 인증(=/admin-hoya/** 보호)은 별도 담당 — 이 컨트롤러는 인증됐다고 가정한다.
 * 화면은 layout/default를 decorate하지 않는 standalone 템플릿(admin/ad/*)을 사용한다.
 */
@Slf4j
@Controller
@RequestMapping("/admin-hoya/ad/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;
    private final AdminAdStatService adminAdStatService;
    private final AdSlotRepository adSlotRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", adminAdStatService.getBannerRows());
        return "admin/ad/banner-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("banner", null);
        addFormOptions(model);
        return "admin/ad/banner-form";
    }

    /** 슬롯 목록 + 광고주명 자동완성 + 슬롯 겹침 경고에 쓸 기존 배너 정보. */
    private void addFormOptions(Model model) {
        model.addAttribute("slots", adSlotRepository.findAll());
        model.addAttribute("advertiserNames", adminAdStatService.getAdvertiserNames());
        model.addAttribute("occupancies", adminAdStatService.getSlotOccupancies());
    }

    @PostMapping
    public String create(
            @RequestParam Long slotId,
            @RequestParam("image") MultipartFile image,
            @RequestParam String linkUrl,
            @RequestParam String advertiserName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endAt,
            @RequestParam(required = false, defaultValue = "false") boolean enabled,
            RedirectAttributes redirectAttributes) {
        try {
            adminBannerService.createBanner(slotId, image, linkUrl, advertiserName, startAt, endAt, enabled);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/banners/new";
        }
        return "redirect:/admin-hoya/ad/banners";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Banner banner;
        try {
            banner = adminBannerService.getBanner(id);
        } catch (AdException e) {
            log.warn("배너 수정 화면 진입 실패: id={}, message={}", id, e.getMessage());
            return "redirect:/admin-hoya/ad/banners";
        }
        model.addAttribute("banner", banner);
        addFormOptions(model);
        return "admin/ad/banner-form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam Long slotId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam String linkUrl,
            @RequestParam String advertiserName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endAt,
            @RequestParam(required = false, defaultValue = "false") boolean enabled,
            RedirectAttributes redirectAttributes) {
        try {
            adminBannerService.updateBanner(id, slotId, image, linkUrl, advertiserName, startAt, endAt, enabled);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-hoya/ad/banners/" + id + "/edit";
        }
        return "redirect:/admin-hoya/ad/banners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminBannerService.deleteBanner(id);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/ad/banners";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminBannerService.toggleBanner(id);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/ad/banners";
    }
}
