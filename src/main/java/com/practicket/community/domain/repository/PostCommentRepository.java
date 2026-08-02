package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /** 페이징을 두지 않는다 — 서버 렌더링이라 크롤러가 댓글까지 봐야 색인 가치가 생긴다 */
    List<PostComment> findByPostOrderByCreatedAtAsc(Post post);

    /** 컨텍스트를 비우는 이유는 {@code PostRepository.incrementReportCount} 와 같다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostComment c SET c.reportCount = c.reportCount + 1 WHERE c.id = :id")
    void incrementReportCount(@Param("id") Long id);
}
