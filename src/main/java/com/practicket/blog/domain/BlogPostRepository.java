package com.practicket.blog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    /** 공개 목록과 sitemap 이 함께 쓴다 — idx_blog_post_list 를 그대로 탄다 */
    List<BlogPost> findByStatusOrderByPublishedAtDescIdDesc(BlogPostStatus status);

    /**
     * 공개 상세. 상태를 함께 조회하지 않으면 URL 만 찍어 미발행 글을 열 수 있다.
     */
    Optional<BlogPost> findByIdAndStatus(Long id, BlogPostStatus status);

    /** 어드민은 미발행 글도 봐야 해서 발행 시각 대신 작성 시각으로 줄 세운다 */
    List<BlogPost> findAllByOrderByCreatedAtDescIdDesc();

    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
