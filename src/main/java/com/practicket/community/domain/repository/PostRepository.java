package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 집계 컬럼은 읽어서 +1 하지 않고 UPDATE 한 방으로 올린다 — 동시에 두 명이 누르면 한 번이 사라진다 */
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /** 컨텍스트를 비우지 않으면 이미 읽어둔 엔티티가 옛 값을 들고 있어 숫자가 한 박자 늦게 보인다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /** 카운터가 음수가 되지 않게 조건을 건다 */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :id AND p.commentCount > 0")
    void decrementCommentCount(@Param("id") Long id);

    /**
     * 컨텍스트를 비워야 한다. 직후에 같은 행을 엔티티로 불러 {@code blind()} 를 호출하는데,
     * 안 비우면 낡은 엔티티가 커밋 때 옛 reportCount 로 방금 올린 값을 덮어쓴다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.reportCount = p.reportCount + 1 WHERE p.id = :id")
    void incrementReportCount(@Param("id") Long id);
}
