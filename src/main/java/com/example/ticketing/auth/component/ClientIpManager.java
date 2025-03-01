package com.example.ticketing.auth.component;

import com.example.ticketing.auth.domain.ClientInfo;
import com.example.ticketing.auth.domain.ClientIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClientIpManager {

    private final ClientIpRepository clientIpRepository;

    public void save(String name, String ip, String device) {
        clientIpRepository.save(ClientInfo.builder()
                .name(name)
                .ip(ip)
                .device(device)
                .build());
    }
}
