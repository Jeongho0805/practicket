package com.example.ticketing.auth;

import com.example.ticketing.auth.component.ClientIpManager;
import com.example.ticketing.auth.component.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientIpResolver clientIpResolver;

    private final ClientIpManager clientIpManager;

    public String recordIp(HttpServletRequest request) {
        String clientIp = clientIpResolver.getClientIp(request);
        clientIpManager.save(clientIp);
        return clientIp;
    }
}
