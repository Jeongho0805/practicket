package com.practicket.art.domain.repository;

import com.practicket.art.domain.entity.Art;
import com.practicket.art.domain.entity.ArtLike;
import com.practicket.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ArtLikeRepository extends JpaRepository<ArtLike, Long> {

    Optional<ArtLike> findByArtAndClient(Art art, Client client);

    @Query("select l.art.id from ArtLike l where l.client.id = :clientId and l.art.id in :artIds")
    Set<Long> findLikedArtIds(@Param("clientId") Long clientId, @Param("artIds") Collection<Long> artIds);

    boolean existsByArtAndClient(Art art, Client client);

    void deleteByArtAndClient(Art art, Client client);

    Long countByArt(Art art);
}