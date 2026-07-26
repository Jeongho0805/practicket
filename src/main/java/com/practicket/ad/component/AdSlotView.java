package com.practicket.ad.component;

import com.practicket.ad.application.AdRenderService;
import com.practicket.ad.domain.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 템플릿(fragment)에서 호출하는 렌더 진입점.
 * 슬롯 code로 노출할 배너를 고르고 노출을 카운트한다. 없으면 null → 템플릿이 애드센스/쿠팡 fallback.
 * 사용: th:with="banner=${@adSlotView.render('PC_LEFT')}"
 */
@Component
@RequiredArgsConstructor
public class AdSlotView {

    private final AdRenderService adRenderService;
    private final AdImpressionCounter adImpressionCounter;

    public Banner render(String slotCode) {
        return adRenderService.pick(slotCode)
                .map(banner -> {
                    adImpressionCounter.record(banner.getId());
                    return banner;
                })
                .orElse(null);
    }
}
