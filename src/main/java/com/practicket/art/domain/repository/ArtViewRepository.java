package com.practicket.art.domain.repository;

import com.practicket.art.domain.entity.Art;
import com.practicket.art.domain.entity.ArtView;
import com.practicket.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtViewRepository extends JpaRepository<ArtView, Long> {

    boolean existsByArtAndClient(Art art, Client client);
}
