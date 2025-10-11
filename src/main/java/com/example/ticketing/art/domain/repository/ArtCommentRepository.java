package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.entity.Art;
import com.example.ticketing.art.domain.entity.ArtComment;
import com.example.ticketing.client.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtCommentRepository extends JpaRepository<ArtComment, Long> {

    Page<ArtComment> findByArtOrderByCreatedAtAsc(Art art, Pageable pageable);

    List<ArtComment> findByArtOrderByCreatedAtAsc(Art art);

    Long countByArt(Art art);

    void deleteByArtAndClient(Art art, Client client);
}