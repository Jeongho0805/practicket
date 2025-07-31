package com.example.ticketing.client.application;

import com.example.ticketing.client.component.ClientInfoExtractor;
import com.example.ticketing.client.component.ClientManager;
import com.example.ticketing.client.domain.Client;
import com.example.ticketing.client.dto.ClientRequestInfo;
import com.example.ticketing.client.dto.ClientUpdateDto;
import com.example.ticketing.client.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ClientService {

    private final ClientInfoExtractor clientInfoExtractor;

    private final ClientManager clientManager;

    public TokenResponse create(HttpServletRequest request) {
        ClientRequestInfo info = clientInfoExtractor.extractClientInfo(request);
        Client client = clientManager.create(info);
        return new TokenResponse(client.getToken());
    }

    public void update(Long clientId, ClientUpdateDto updateDto) {
        clientManager.updateById(clientId, updateDto);
    }
}
