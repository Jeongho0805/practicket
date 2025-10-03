package com.example.ticketing.art.domain;

import com.example.ticketing.client.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtRepository extends JpaRepository<Art, Long> {

    Page<Art> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Art> findByIsPublicTrueOrderByLikeCountDescCreatedAtDesc(Pageable pageable);

    Page<Art> findByIsPublicTrueOrderByViewCountDescCreatedAtDesc(Pageable pageable);

    Page<Art> findByClientAndIsPublicTrueOrderByCreatedAtDesc(Client client, Pageable pageable);

    @Query("SELECT a FROM Art a WHERE a.isPublic = true AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Art> findByKeywordAndIsPublicTrue(@Param("keyword") String keyword, Pageable pageable);

    Optional<Art> findByIdAndIsPublicTrue(Long id);

    List<Art> findTop10ByIsPublicTrueOrderByLikeCountDescCreatedAtDesc();

    @Query("SELECT COUNT(a) FROM Art a WHERE a.client = :client")
    Long countByClient(@Param("client") Client client);
}