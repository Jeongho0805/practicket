package com.example.ticketing.client.component;

import com.example.ticketing.client.dto.ClientRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClientInfoExtractor {

    private static final String IP_KEY = "X-Forwarded-For";
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

    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader(IP_KEY);
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
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
