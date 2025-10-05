package com.example.ticketing.client.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByToken(String token);

    List<Client> findByNameContainingIgnoreCase(String name);

    @Modifying(clearAutomatically = true)
    @Query("update Client c set c.name = :name where c.id = :clientId")
    void updateNameById(@Param("clientId") Long clientId, @Param("name") String name);
}
