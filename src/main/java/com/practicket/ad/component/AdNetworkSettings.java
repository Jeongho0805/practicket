package com.practicket.ad.component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /** 화면에 보이는 이름. DB 와 렌더는 코드를 쓰고 사람은 한글을 본다 */
    public static final Map<String, String> LABELS = labels();

    private static Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(COUPANG, "쿠팡 파트너스");
        labels.put(ADSENSE, "구글 애드센스");
        labels.put(ADFIT, "카카오 애드핏");
        return Collections.unmodifiableMap(labels);
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
