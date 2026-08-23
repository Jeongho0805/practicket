package com.practicket.notice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 고정 먼저, 그 다음 최신순 — idx_notice_list 를 그대로 탄다 */
    Page<Notice> findByPublishedTrueOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    /** 필터 탭(공지/수정)용 */
    Page<Notice> findByPublishedTrueAndTypeOrderByPinnedDescCreatedAtDesc(NoticeType type, Pageable pageable);

    long countByPublishedTrue();

    long countByPublishedTrueAndType(NoticeType type);

    Optional<Notice> findByIdAndPublishedTrue(Long id);

    /** 어드민은 비공개 글도 봐야 한다 */
    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    /** sitemap 의 /notice lastmod — 공개된 공지 중 가장 최근 것 */
    Optional<Notice> findTopByPublishedTrueOrderByCreatedAtDesc();
}
