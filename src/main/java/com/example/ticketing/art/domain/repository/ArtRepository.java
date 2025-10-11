package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.entity.Art;
import com.example.ticketing.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtRepository extends JpaRepository<Art, Long>, ArtRepositoryCustom {

    @Query("SELECT COUNT(a) FROM Art a WHERE a.client = :client")
    Long countByClient(@Param("client") Client client);

    @Modifying
    @Query("UPDATE Art a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Art a SET a.likeCount = a.likeCount - 1 WHERE a.id = :id AND a.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Art a SET a.commentCount = a.commentCount + 1 WHERE a.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Art a SET a.commentCount = a.commentCount - 1 WHERE a.id = :id AND a.commentCount > 0")
    void decrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Art a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
}