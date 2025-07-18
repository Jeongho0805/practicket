package com.example.ticketing.auth.application;

import com.example.ticketing.auth.component.ClientInfoManager;
import com.example.ticketing.auth.component.ClientInfoExtractor;
import com.example.ticketing.auth.component.SessionManager;
import com.example.ticketing.auth.component.TokenManager;
import com.example.ticketing.auth.dto.ClientInfo;
import com.example.ticketing.auth.dto.SessionObject;
import com.example.ticketing.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientInfoExtractor clientInfoExtractor;

    private final ClientInfoManager clientIpManager;

    private final SessionManager sessionManager;

    private final TokenManager tokenManager;

    public SessionObject getSessionObject(HttpServletRequest request) {
        return sessionManager.getSessionObject(request);
    }

    public TokenResponse createToken(HttpServletRequest request) {
        ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(request);
        String token = tokenManager.createToken(clientInfo);
        return new TokenResponse(token);
    }

    public void deleteSession(HttpServletRequest request) {
        SessionManager.deleteSession(request);
    }
}
