package com.practicket.client.component;

import com.practicket.client.dto.ClientRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClientInfoExtractor {

    private static final String IP_KEY = "X-Forwarded-For";
    private static final String DEVICE_KEY = "User-Agent";
    private static final String SOURCE_URL_KEY = "Referer";

    // client 테이블 ip/device/referer 컬럼은 VARCHAR(255). 초과 입력 시 Data truncation 으로 INSERT 실패하므로 저장 전 절단한다. (PRACTICKET-23)
    private static final int COLUMN_MAX_LENGTH = 255;

    public ClientRequestInfo extractClientInfo(HttpServletRequest request) {
        String ip = this.extractClientIp(request);
        String device = this.extractClientDevice(request);
        String sourceUrl = this.extractClientReferer(request);

        return ClientRequestInfo.builder()
                .ip(truncate(ip))
                .device(truncate(device))
                .sourceUrl(truncate(sourceUrl))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader(IP_KEY);
        if (clientIp != null && !clientIp.isEmpty()) {
            return clientIp.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractClientDevice(HttpServletRequest request) {
        String userAgent = request.getHeader(DEVICE_KEY);
        if (userAgent == null) {
            userAgent = "Unknown Device";
        }
        return userAgent;
    }

    private String extractClientReferer(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(SOURCE_URL_KEY)).orElse("Unknown");
    }

    private String truncate(String value) {
        if (value != null && value.length() > COLUMN_MAX_LENGTH) {
            return value.substring(0, COLUMN_MAX_LENGTH);
        }
        return value;
    }
}
