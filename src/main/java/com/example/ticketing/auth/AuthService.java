package com.example.ticketing.auth;

import com.example.ticketing.auth.component.ClientInfoManager;
import com.example.ticketing.auth.component.ClientExtractor;
import com.example.ticketing.auth.component.SessionManager;
import com.example.ticketing.auth.dto.LoginRequestDto;
import com.example.ticketing.auth.dto.SessionObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientExtractor clientExtractor;

    private final ClientInfoManager clientIpManager;

    private final SessionManager sessionManager;

    public SessionObject getSessionObject(HttpServletRequest request) {
        return sessionManager.getSessionObject(request);
    }

    public void createSession(HttpServletRequest request, LoginRequestDto dto) {
        String ip = clientExtractor.extractClientIp(request);
        String device = clientExtractor.extractClientDevice(request);
        String sessionKey = sessionManager.createSession(request, ip, dto.getName());
        clientIpManager.save(dto.getName(), ip, device, sessionKey);
    }

    public void deleteSession(HttpServletRequest request) {
        SessionManager.deleteSession(request);
    }
}
