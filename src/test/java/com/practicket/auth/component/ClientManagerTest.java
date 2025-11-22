package com.practicket.auth.component;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.client.dto.ClientRequestInfo;
import com.practicket.client.dto.ClientUpdateDto;
import com.practicket.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, ClientManager.class})
@ActiveProfiles("test")
public class ClientManagerTest {

    @Autowired
    ClientManager clientManager;

    @Autowired
    ClientRepository clientRepository;

    @Test
    @DisplayName("토큰을 생성하고 Client 정보가 저장된다")
    void createTest() {
        // given
        ClientRequestInfo request = ClientRequestInfo.builder()
                .ip("127.0.0.1")
                .device("Chrome")
                .sourceUrl("https://example.com")
                .build();

        // when
        Client savedClient = clientManager.create(request);

        // then
        assertThat(savedClient.getIp()).isEqualTo(request.getIp());
        assertThat(savedClient.getDevice()).isEqualTo(request.getDevice());
        assertThat(savedClient.getReferer()).isEqualTo(request.getSourceUrl());
        assertThat(savedClient.getBanned()).isFalse();
        assertThat(savedClient.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 이름 정보가 업테이트 된다.")
    void updateByIdTest() {
        // given
        ClientRequestInfo request = ClientRequestInfo.builder()
                .ip("127.0.0.1")
                .device("Chrome")
                .sourceUrl("https://example.com")
                .build();
        Client savedClient = clientManager.create(request);

        // when
        ClientUpdateDto updateDto = new ClientUpdateDto("test");
        clientManager.updateById(savedClient.getId(), updateDto);

        // then
        Client client = clientRepository.findById(savedClient.getId()).orElseThrow(() -> new IllegalArgumentException());
        assertThat(client.getName()).isEqualTo(updateDto.getName());
    }
}
