package com.practicket.ad.domain;

import java.time.LocalDate;

/**
 * 어드민 화면에 표시하는 배너 상태. enabled 플래그와 게재 기간을 합쳐 하나로 본다.
 * 기간을 비운 배너는 소속 계약의 기간을 따르므로 판정에도 계약을 함께 넘긴다.
 */
public enum BannerStatus {

    LIVE("live", "노출중"),
    WAIT("wait", "예약"),
    DONE("done", "종료"),
    OFF("off", "중지"),
    DELETED("deleted", "삭제");

    private final String key;
    private final String label;

    BannerStatus(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static BannerStatus of(Banner banner, AdCampaign campaign, LocalDate today) {
        if (banner.isDeleted()) {
            return DELETED;
        }
        if (!Boolean.TRUE.equals(banner.getEnabled())) {
            return OFF;
        }
        LocalDate start = effectiveStart(banner, campaign);
        LocalDate end = effectiveEnd(banner, campaign);
        if (start != null && start.isAfter(today)) {
            return WAIT;
        }
        if (end != null && end.isBefore(today)) {
            return DONE;
        }
        return LIVE;
    }

    public static LocalDate effectiveStart(Banner banner, AdCampaign campaign) {
        if (banner.getStartAt() != null) {
            return banner.getStartAt();
        }
        return campaign == null ? null : campaign.getStartAt();
    }

    public static LocalDate effectiveEnd(Banner banner, AdCampaign campaign) {
        if (banner.getEndAt() != null) {
            return banner.getEndAt();
        }
        return campaign == null ? null : campaign.getEndAt();
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
