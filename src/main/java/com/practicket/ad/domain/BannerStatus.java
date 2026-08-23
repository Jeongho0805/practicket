package com.practicket.ad.domain;

import java.time.LocalDate;

/**
 * 어드민 화면에 표시하는 배너 상태. enabled 플래그와 게재 기간을 합쳐 하나로 본다.
 * 실제 렌더 조건({@code BannerRepository#findActiveBySlotCode})과 같은 기준이라
 * LIVE = 지금 실제로 노출될 수 있는 상태를 뜻한다(슬롯 비활성은 슬롯 화면에서 따로 확인).
 */
public enum BannerStatus {

    LIVE("live", "노출중"),
    WAIT("wait", "예약"),
    DONE("done", "종료"),
    OFF("off", "중지");

    private final String key;
    private final String label;

    BannerStatus(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static BannerStatus of(Banner banner, LocalDate today) {
        if (!Boolean.TRUE.equals(banner.getEnabled())) {
            return OFF;
        }
        if (banner.getStartAt() != null && banner.getStartAt().isAfter(today)) {
            return WAIT;
        }
        if (banner.getEndAt() != null && banner.getEndAt().isBefore(today)) {
            return DONE;
        }
        return LIVE;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
