package com.practicket.ad.component;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 한 자리를 기기 하나가 볼 때의 결과. 자리는 데스크톱과 모바일에서 서로 다른 결과를 낼 수 있다
 * (판 배너가 데스크톱 그림만 가진 경우 등).
 */
@Getter
@AllArgsConstructor
public class AdFace {

    private static final AdFace NONE = new AdFace(false, false, false,
            null, null, null, null, null, null, null, null);

    private final boolean banner;
    private final boolean fill;
    /** 채울 자리는 맞지만 지금 프로파일에서는 네트워크 코드를 안 내보낸다 */
    private final boolean fillEnabled;

    private final Long bannerId;
    private final String advertiserName;
    private final String imagePath;

    private final String network;
    private final String unitId;
    /** 네트워크 계정 값. 애드센스는 client, 쿠팡은 trackingCode */
    private final String account;
    private final String template;
    private final String size;

    public static AdFace none() {
        return NONE;
    }

    public static AdFace banner(Long bannerId, String advertiserName, String imagePath) {
        return new AdFace(true, false, false, bannerId, advertiserName, imagePath,
                null, null, null, null, null);
    }

    public static AdFace fill(boolean enabled, AdSlotSnapshot.Unit unit,
                              String account, String template) {
        return new AdFace(false, true, enabled, null, null, null,
                unit.network(), unit.unitId(), account, template, sizeOf(unit));
    }

    /** 규격을 코드에 박아야 하는 네트워크(애드핏)만 값이 있다. 반응형이면 빈 값이다 */
    private static String sizeOf(AdSlotSnapshot.Unit unit) {
        if (unit.width() == null || unit.height() == null) {
            return null;
        }
        return unit.width() + "x" + unit.height();
    }
}
