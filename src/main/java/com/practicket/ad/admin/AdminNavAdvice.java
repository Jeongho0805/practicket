package com.practicket.ad.admin;

import com.practicket.ad.application.AdminBannerController;
import com.practicket.ad.application.AdminDashboardController;
import com.practicket.ad.application.AdminSlotController;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 사이드바 배지 숫자를 모든 어드민 화면에 공급한다. 컨트롤러마다 같은 카운트를 담지 않으려는 목적.
 * 대상 테이블이 작아 매 요청 count 쿼리로 충분하다.
 */
@ControllerAdvice(assignableTypes = {
        AdminDashboardController.class,
        AdminBannerController.class,
        AdminSlotController.class
})
@RequiredArgsConstructor
public class AdminNavAdvice {

    private final BannerRepository bannerRepository;
    private final AdSlotRepository adSlotRepository;

    @ModelAttribute("navBannerCount")
    public long navBannerCount() {
        return bannerRepository.count();
    }

    @ModelAttribute("navSlotCount")
    public long navSlotCount() {
        return adSlotRepository.count();
    }
}
