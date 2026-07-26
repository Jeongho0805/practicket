package com.practicket.ad.component;

import com.practicket.ad.application.AdRenderService;
import com.practicket.ad.domain.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 템플릿(fragment)에서 호출하는 렌더 진입점. 슬롯 code로 노출할 배너를 고르기만 한다.
 * 없으면 null → 템플릿이 애드센스/쿠팡 fallback.
 * 사용: th:with="banner=${@adSlotView.peek('PC_LEFT')}"
 *
 * 노출 집계는 여기서 하지 않는다. 화면 폭에 따라 실제로 보이는 배너만 클라이언트(/js/ad-metrics.js)가
 * 보고하므로, 서버 렌더 시점에 세면 안 보인 배너까지 집계된다.
 */
@Component
@RequiredArgsConstructor
public class AdSlotView {

    private final AdRenderService adRenderService;

    public Banner peek(String slotCode) {
        return adRenderService.pick(slotCode).orElse(null);
    }
}
