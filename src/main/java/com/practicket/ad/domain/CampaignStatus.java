package com.practicket.ad.domain;

import java.time.LocalDate;

/**
 * 계약 상태. 저장하지 않고 기간과 오늘을 비교해 만든다 —
 * 저장하면 날짜가 지나도 값이 그대로 남아 화면과 어긋나는 순간이 반드시 온다.
 */
public enum CampaignStatus {

    LIVE("live", "진행"),
    WAIT("wait", "예정"),
    DONE("done", "종료");

    private final String key;
    private final String label;

    CampaignStatus(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static CampaignStatus of(AdCampaign campaign, LocalDate today) {
        if (campaign.isUpcoming(today)) {
            return WAIT;
        }
        return campaign.isFinished(today) ? DONE : LIVE;
    }

    /** 진행 → 예정 → 종료 순으로 세울 때 쓴다 */
    public int getOrder() {
        return ordinal();
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
