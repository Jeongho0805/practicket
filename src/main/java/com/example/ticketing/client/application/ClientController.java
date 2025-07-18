package com.example.ticketing.client.application;

import com.example.ticketing.client.dto.ClientResponse;
import com.example.ticketing.client.dto.ClientUpdateDto;
import com.example.ticketing.client.dto.TokenResponse;
import com.example.ticketing.common.auth.Auth;
import com.example.ticketing.common.auth.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<ClientResponse> findClient(@Auth ClientInfo clientInfo) {
        ClientResponse clientResponse = new ClientResponse(clientInfo.getName(), clientInfo.getBanned());
        return ResponseEntity.ok(clientResponse);
    }

    @PostMapping
    public ResponseEntity<TokenResponse> create(HttpServletRequest request) {
        TokenResponse token = clientService.create(request);
        return ResponseEntity.ok(token);
    }

    @PatchMapping
    public ResponseEntity<Void> update(@Auth ClientInfo clientInfo, @Valid @RequestBody ClientUpdateDto updateDto) {
        clientService.update(clientInfo.getClientId(), updateDto);
        return ResponseEntity.ok().build();
    }
}
