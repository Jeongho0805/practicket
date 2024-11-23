package com.example.ticketing.auth.component;

import com.example.ticketing.auth.domain.ClientIp;
import com.example.ticketing.auth.domain.ClientIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClientIpManager {

    private final ClientIpRepository clientIpRepository;

    public void save(String value) {
        LocalDateTime now = LocalDateTime.now();
        ClientIp clientIp = new ClientIp(value, now);
        clientIpRepository.save(clientIp);
    }
}
