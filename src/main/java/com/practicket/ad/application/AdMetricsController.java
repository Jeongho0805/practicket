package com.practicket.ad.application;

import com.practicket.ad.component.AdImpressionCounter;
import com.practicket.ad.component.AdTrafficFilter;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 배너 노출 집계 수신. 실제로 화면에 보인 배너만 클라이언트가 1회 보고한다(/js/ad-metrics.js).
 *
 * 경로에 'ad'를 넣지 않는 이유: 광고 차단기가 /ad/* 패턴을 막아 집계가 통째로 누락된다.
 * 응답은 항상 204 — 유효하지 않은 요청도 조용히 흘려보내 탐색 정보를 주지 않는다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdMetricsController {

    private final BannerRepository bannerRepository;
    private final AdImpressionCounter adImpressionCounter;
    private final AdTrafficFilter adTrafficFilter;

    @PostMapping("/metrics/v/{bannerId}")
    public ResponseEntity<Void> view(@PathVariable Long bannerId, HttpServletRequest request) {
        if (adTrafficFilter.isExcluded(request)) {
            return ResponseEntity.noContent().build();
        }

        Optional<Banner> banner = bannerRepository.findById(bannerId);
        if (banner.isEmpty() || !Boolean.TRUE.equals(banner.get().getEnabled())) {
            log.debug("노출 집계 대상 아님: bannerId={}", bannerId);
            return ResponseEntity.noContent().build();
        }

        adImpressionCounter.record(bannerId);
        return ResponseEntity.noContent().build();
    }
}
