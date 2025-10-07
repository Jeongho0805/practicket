package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.entity.Art;
import com.example.ticketing.art.domain.entity.ArtView;
import com.example.ticketing.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtViewRepository extends JpaRepository<ArtView, Long> {

    boolean existsByArtAndClient(Art art, Client client);
}
