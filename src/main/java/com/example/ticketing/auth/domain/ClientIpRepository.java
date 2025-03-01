package com.example.ticketing.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientIpRepository extends JpaRepository<ClientInfo, Long> {}
