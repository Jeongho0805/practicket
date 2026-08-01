package com.practicket.community.admin.repository;

import com.practicket.community.admin.dto.AdminCommentView;
import com.practicket.community.domain.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** {@link AdminPostQueryRepository} 와 같은 이유로 네이티브 쿼리 + 프로젝션을 쓴다 */
public interface AdminPostCommentQueryRepository extends Repository<PostComment, Long> {

    @Query(value = """
            SELECT id, post_id AS postId, client_id AS clientId, content, nickname, ip,
                   report_count AS reportCount, blinded, created_at AS createdAt, deleted_at AS deletedAt
            FROM post_comment
            WHERE id = :id
            """, nativeQuery = true)
    Optional<AdminCommentView> findAdminViewById(@Param("id") Long id);

    /** 삭제된 댓글도 그대로 나온다 */
    @Query(value = """
            SELECT id, post_id AS postId, client_id AS clientId, content, nickname, ip,
                   report_count AS reportCount, blinded, created_at AS createdAt, deleted_at AS deletedAt
            FROM post_comment
            WHERE (:keyword = '' OR content LIKE CONCAT('%', :keyword, '%') OR nickname LIKE CONCAT('%', :keyword, '%'))
            ORDER BY id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM post_comment
            WHERE (:keyword = '' OR content LIKE CONCAT('%', :keyword, '%') OR nickname LIKE CONCAT('%', :keyword, '%'))
            """,
            nativeQuery = true)
    Page<AdminCommentView> search(@Param("keyword") String keyword, Pageable pageable);
}
