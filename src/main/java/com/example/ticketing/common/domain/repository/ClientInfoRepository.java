package com.example.ticketing.common.domain.repository;

import com.example.ticketing.common.domain.entity.ClientInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientInfoRepository extends JpaRepository<ClientInfo, Long> {

    ClientInfo findClientInfoBySessionKey(String sessionKey);
}
