package com.example.ticketing.auth.component;

import com.example.ticketing.common.domain.entity.ClientInfo;
import com.example.ticketing.common.domain.repository.ClientInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientInfoManager {

    private final ClientInfoRepository clientInfoRepository;

    public ClientInfo findBySessionKey(String sessionKey) {
        return clientInfoRepository.findClientInfoBySessionKey(sessionKey);
    }

    public void save(String name, String ip, String device, String sessionKey) {
        clientInfoRepository.save(ClientInfo.builder()
                .name(name)
                .ip(ip)
                .device(device)
                .sessionKey(sessionKey)
                .build());
    }
}
