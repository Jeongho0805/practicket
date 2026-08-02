package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT b FROM Banner b JOIN FETCH b.slot WHERE b.reportToken = :reportToken")
    Optional<Banner> findByReportToken(@Param("reportToken") String reportToken);

    /**
     * 어드민 목록/수정 화면은 slot 정보까지 렌더하는데 open-in-view=false 라 뷰에서 프록시 초기화가 안 된다.
     * 따라서 조회 시점에 slot을 함께 가져온다.
     */
    @Query("SELECT b FROM Banner b JOIN FETCH b.slot ORDER BY b.createdAt DESC")
    List<Banner> findAllWithSlotOrderByCreatedAtDesc();

    @Query("SELECT b FROM Banner b JOIN FETCH b.slot WHERE b.id = :id")
    Optional<Banner> findWithSlotById(@Param("id") Long id);

    @Query("SELECT b FROM Banner b WHERE b.enabled = true AND b.slot.enabled = true " +
            "AND b.slot.code = :code AND b.startAt <= :today AND b.endAt >= :today")
    List<Banner> findActiveBySlotCode(@Param("code") String code, @Param("today") LocalDate today);
}
