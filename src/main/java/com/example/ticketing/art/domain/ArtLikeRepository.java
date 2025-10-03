package com.example.ticketing.art.domain;

import com.example.ticketing.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtLikeRepository extends JpaRepository<ArtLike, Long> {

    Optional<ArtLike> findByArtAndClient(Art art, Client client);

    boolean existsByArtAndClient(Art art, Client client);

    void deleteByArtAndClient(Art art, Client client);

    Long countByArt(Art art);
}