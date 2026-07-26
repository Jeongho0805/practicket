package com.practicket.ad.application;

import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.exception.AdException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 어드민 슬롯 관리 — 목록 조회, enable/disable 토글, recommendedSize 편집(간단히).
 * 슬롯 신규 생성/삭제는 범위 밖(운영자가 미리 시드 등록 — docs/ad-admin-system.md Q3).
 * AdSlot 엔티티는 setter가 없어(불변 스타일) 수정 시 builder로 새 상태를 만들어 save(merge)한다.
 */
@Controller
@RequestMapping("/admin-hoya/ad/slots")
@RequiredArgsConstructor
public class AdminSlotController {

    private final AdSlotRepository adSlotRepository;
    private final AdminAdStatService adminAdStatService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", adminAdStatService.getSlotRows());
        return "admin/ad/slot-list";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        AdSlot slot = findSlotOrThrow(id);
        AdSlot updated = AdSlot.builder()
                .id(slot.getId())
                .code(slot.getCode())
                .name(slot.getName())
                .recommendedSize(slot.getRecommendedSize())
                .enabled(!Boolean.TRUE.equals(slot.getEnabled()))
                .createdAt(slot.getCreatedAt())
                .build();
        try {
            adSlotRepository.save(updated);
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/ad/slots";
    }

    @PostMapping("/{id}")
    public String updateRecommendedSize(@PathVariable Long id,
                                         @RequestParam String recommendedSize,
                                         RedirectAttributes redirectAttributes) {
        AdSlot slot = findSlotOrThrow(id);
        AdSlot updated = AdSlot.builder()
                .id(slot.getId())
                .code(slot.getCode())
                .name(slot.getName())
                .recommendedSize(recommendedSize)
                .enabled(slot.getEnabled())
                .createdAt(slot.getCreatedAt())
                .build();
        adSlotRepository.save(updated);
        return "redirect:/admin-hoya/ad/slots";
    }

    private AdSlot findSlotOrThrow(Long id) {
        return adSlotRepository.findById(id)
                .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));
    }
}
