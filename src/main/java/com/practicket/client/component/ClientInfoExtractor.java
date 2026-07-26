package com.practicket.client.component;

import com.practicket.client.dto.ClientRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClientInfoExtractor {

    private static final String DEVICE_KEY = "User-Agent";
    private static final String SOURCE_URL_KEY = "Referer";

    public ClientRequestInfo extractClientInfo(HttpServletRequest request) {
        String ip = this.extractClientIp(request);
        String device = this.extractClientDevice(request);
        String sourceUrl = this.extractClientReferer(request);

        return ClientRequestInfo.builder()
                .ip(ip)
                .device(device)
                .sourceUrl(sourceUrl)
                .build();
    }

    // X-Forwarded-For 를 직접 읽지 않는다. 헤더는 클라이언트가 위조할 수 있어
    // 앱 단에서는 프록시가 붙인 값인지 구분할 수 없다.
    // server.forward-headers-strategy=native 로 켜지는 톰캣 RemoteIpValve 가
    // 신뢰 프록시를 걷어낸 뒤 실제 클라이언트 IP 를 여기에 넣어준다.
    private String extractClientIp(HttpServletRequest request) {
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
}
