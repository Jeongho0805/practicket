package com.practicket.community.admin.repository;

import com.practicket.community.admin.dto.AdminPostView;
import com.practicket.community.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 삭제된 글까지 보는 어드민 전용 조회. Post 의 {@code @SQLRestriction} 은 엔티티를 하이드레이트할 때만
 * 걸리므로, 엔티티가 아니라 {@link AdminPostView} 로 바로 매핑해 우회한다.
 * 마커 인터페이스만 확장해 SQLRestriction 이 걸리는 기본 CRUD 는 노출하지 않는다.
 */
public interface AdminPostQueryRepository extends Repository<Post, Long> {

    @Query(value = """
            SELECT id, client_id AS clientId, title, content, nickname, ip,
                   view_count AS viewCount, like_count AS likeCount, comment_count AS commentCount,
                   report_count AS reportCount, blinded, edited, created_at AS createdAt, deleted_at AS deletedAt
            FROM post
            WHERE id = :id
            """, nativeQuery = true)
    Optional<AdminPostView> findAdminViewById(@Param("id") Long id);

    /** keyword 가 빈 문자열이면 전체를 반환한다 */
    @Query(value = """
            SELECT id, client_id AS clientId, title, content, nickname, ip,
                   view_count AS viewCount, like_count AS likeCount, comment_count AS commentCount,
                   report_count AS reportCount, blinded, edited, created_at AS createdAt, deleted_at AS deletedAt
            FROM post
            WHERE (:keyword = '' OR title LIKE CONCAT('%', :keyword, '%') OR nickname LIKE CONCAT('%', :keyword, '%'))
            ORDER BY id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM post
            WHERE (:keyword = '' OR title LIKE CONCAT('%', :keyword, '%') OR nickname LIKE CONCAT('%', :keyword, '%'))
            """,
            nativeQuery = true)
    Page<AdminPostView> search(@Param("keyword") String keyword, Pageable pageable);
}
