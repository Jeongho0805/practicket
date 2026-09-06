package com.practicket.ad.component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 네트워크 계정 값. 광고단위와 달리 사이트에 하나뿐이라 DB 가 아니라 설정에 둔다.
 * 프로파일별 파일은 git 에 안 올라가므로 공통 application.yml 이 가진다.
 */
@Getter
@Component
public class AdNetworkSettings {

    public static final String ADSENSE = "ADSENSE";
    public static final String COUPANG = "COUPANG";
    public static final String ADFIT = "ADFIT";

    private final String adsenseClient;
    private final String coupangTrackingCode;
    private final String coupangTemplate;

    public AdNetworkSettings(@Value("${ad.adsense.client:}") String adsenseClient,
                             @Value("${ad.coupang.tracking-code:}") String coupangTrackingCode,
                             @Value("${ad.coupang.template:carousel}") String coupangTemplate) {
        this.adsenseClient = adsenseClient;
        this.coupangTrackingCode = coupangTrackingCode;
        this.coupangTemplate = coupangTemplate;
    }

    public String accountOf(String network) {
        if (ADSENSE.equals(network)) {
            return adsenseClient;
        }
        if (COUPANG.equals(network)) {
            return coupangTrackingCode;
        }
        return null;
    }

    public String templateOf(String network) {
        return COUPANG.equals(network) ? coupangTemplate : null;
    }
}
