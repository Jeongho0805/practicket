package com.practicket.ad.application;

import com.practicket.ad.component.AdClickCounter;
import com.practicket.ad.component.AdTrafficFilter;
import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdCampaignRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 배너 클릭 → 클릭 카운트 → UTM 파라미터 붙여 실제 링크로 302 리다이렉트.
 * docs/ad-admin-system.md Q4/Q5.
 */
@RestController
@RequiredArgsConstructor
public class AdClickController {

    private final BannerRepository bannerRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final AdClickCounter adClickCounter;
    private final AdTrafficFilter adTrafficFilter;

    @GetMapping("/ad/click/{bannerId}")
    public ResponseEntity<Void> click(@PathVariable Long bannerId, HttpServletRequest request) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));

        // 봇·프리페치는 사용자 클릭이 아니다. 리다이렉트는 정상 수행하되 집계에서만 뺀다.
        if (!adTrafficFilter.isExcluded(request)) {
            adClickCounter.record(bannerId, resolveClientIp(request));
        }

        AdCampaign campaign = banner.getCampaignId() == null ? null
                : adCampaignRepository.findById(banner.getCampaignId()).orElse(null);

        String redirectUrl = withUtm(banner, campaign);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    /** 배너가 링크와 이름을 비우면 계약을 따른다. 계약도 없으면 이관 전 데이터라 배너 값만 남는다. */
    private String withUtm(Banner banner, AdCampaign campaign) {
        String linkUrl = firstNotBlank(banner.getLinkUrl(), campaign == null ? null : campaign.getLinkUrl());
        if (linkUrl == null) {
            return "/";
        }

        String utmParams = "utm_source=practicket&utm_medium=banner&utm_campaign="
                + URLEncoder.encode(campaignName(banner, campaign), StandardCharsets.UTF_8);
        String separator = linkUrl.contains("?") ? "&" : "?";
        return linkUrl + separator + utmParams;
    }

    private String campaignName(Banner banner, AdCampaign campaign) {
        if (campaign != null) {
            String advertiserName = advertiserRepository.findById(campaign.getAdvertiserId())
                    .map(Advertiser::getName).orElse(null);
            String name = firstNotBlank(advertiserName, campaign.getName());
            if (name != null) {
                return name;
            }
        }
        String legacy = firstNotBlank(banner.getAdvertiserName(), null);
        return legacy != null ? legacy : String.valueOf(banner.getId());
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
