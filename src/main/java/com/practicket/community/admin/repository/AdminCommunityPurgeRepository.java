package com.practicket.community.admin.repository;

import com.practicket.community.domain.entity.Post;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 개인정보처리방침이 약속한 "요청 시 실제 파기"를 수행한다. 지우려는 행은 이미 soft delete 된 것이라
 * {@code @SQLRestriction} 때문에 엔티티로 불러올 수 없다 — 그래서 네이티브 DELETE 로만 지운다.
 */
public interface AdminCommunityPurgeRepository extends Repository<Post, Long> {

    @Modifying
    @Query(value = """
            DELETE FROM post_report
            WHERE target_type = 'COMMENT'
              AND target_id IN (SELECT id FROM post_comment WHERE post_id = :postId)
            """, nativeQuery = true)
    void deleteCommentReportsOfPost(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_report WHERE target_type = :targetType AND target_id = :targetId",
            nativeQuery = true)
    void deleteReports(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Modifying
    @Query(value = "DELETE FROM post_like WHERE post_id = :postId", nativeQuery = true)
    void deleteLikesOfPost(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_tag WHERE post_id = :postId", nativeQuery = true)
    void deleteTagsOfPost(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_comment WHERE post_id = :postId", nativeQuery = true)
    void deleteCommentsOfPost(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post WHERE id = :postId", nativeQuery = true)
    void deletePost(@Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_comment WHERE id = :commentId", nativeQuery = true)
    void deleteComment(@Param("commentId") Long commentId);
}
