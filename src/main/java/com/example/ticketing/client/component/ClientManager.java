package com.example.ticketing.client.component;

import com.example.ticketing.client.domain.Client;
import com.example.ticketing.client.domain.ClientRepository;
import com.example.ticketing.client.dto.ClientRequestInfo;
import com.example.ticketing.client.dto.ClientUpdateDto;
import com.example.ticketing.common.exception.AuthException;
import com.example.ticketing.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientManager {

    private final ClientRepository clientRepository;

    public Client findById(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new AuthException(ErrorCode.NOT_FOUND_CLIENT));
    }

    public Client create(ClientRequestInfo clientInfo) {
        String token = UUID.randomUUID().toString();

        Client client = Client.builder()
                .token(token)
                .ip(clientInfo.getIp())
                .device(clientInfo.getDevice())
                .referer(clientInfo.getSourceUrl())
                .banned(false)
                .build();

        return clientRepository.save(client);
    }

    public void updateById(Long clientId, ClientUpdateDto updateDto) {
        clientRepository.updateNameById(clientId, updateDto.getName());
    }
}
