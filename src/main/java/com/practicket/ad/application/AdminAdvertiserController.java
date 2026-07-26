package com.practicket.ad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 광고주 단위 성과. 별도 테이블 없이 배너의 advertiserName으로 묶는다
 * (테이블 승격 판단은 docs/ad-admin-system.md 참고 — 사명 변경·광고주 정보 저장이 필요해지는 시점).
 *
 * 상세는 이름을 경로가 아니라 쿼리 파라미터로 받는다. 한글·공백·슬래시가 섞인 이름을
 * 경로에 넣으면 인코딩 사고가 나기 쉬워서다.
 */
@Controller
@RequestMapping("/admin-hoya/ad/advertisers")
@RequiredArgsConstructor
public class AdminAdvertiserController {

    private final AdminAdStatService adminAdStatService;

    @GetMapping
    public String list(@RequestParam(required = false) String name, Model model) {
        if (name == null || name.isBlank()) {
            model.addAttribute("rows", adminAdStatService.getAdvertiserRows());
            return "admin/ad/advertiser-list";
        }

        return adminAdStatService.findAdvertiser(name)
                .map(advertiser -> {
                    model.addAttribute("advertiser", advertiser);
                    return "admin/ad/advertiser-detail";
                })
                .orElse("redirect:/admin-hoya/ad/advertisers");
    }
}
