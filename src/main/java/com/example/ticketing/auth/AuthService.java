package com.example.ticketing.auth;

import com.example.ticketing.auth.component.ClientIpManager;
import com.example.ticketing.auth.component.ClientIpResolver;
import com.example.ticketing.auth.component.SessionManager;
import com.example.ticketing.auth.dto.LoginRequestDto;
import com.example.ticketing.auth.dto.SessionObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientIpResolver clientIpResolver;

    private final ClientIpManager clientIpManager;

    private final SessionManager sessionManager;

    public SessionObject getSessionObject(HttpServletRequest request) {
        return sessionManager.getSessionObject(request);
    }

    public void createSession(HttpServletRequest request, LoginRequestDto dto) {
        String clientIp = clientIpResolver.extractClientIp(request);
        clientIpManager.save(clientIp);
        sessionManager.createSession(request, clientIp, dto.getName());
    }

    public void deleteSession(HttpServletRequest request) {
        SessionManager.deleteSession(request);
    }
}
