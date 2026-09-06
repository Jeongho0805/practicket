package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

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

    /**
     * 렌더 스냅샷용. 기간 판정은 계약 기간까지 봐야 해서 SQL 로 못 자른다(계약이 연관이 아니라 컬럼이다).
     * 켜진 배너 전부를 한 번에 가져와 애플리케이션이 거른다.
     */
    @Query("SELECT b FROM Banner b JOIN FETCH b.slot WHERE b.enabled = true")
    List<Banner> findAllEnabledWithSlot();

    List<Banner> findByCampaignId(Long campaignId);

    /** 자리 이름까지 함께 그리는 화면용. open-in-view=false 라 뷰에서 프록시 초기화가 안 된다 */
    @Query("SELECT b FROM Banner b JOIN FETCH b.slot WHERE b.campaignId = :campaignId")
    List<Banner> findByCampaignIdWithSlot(@Param("campaignId") Long campaignId);

    List<Banner> findByCampaignIdIn(List<Long> campaignIds);

    long countByCampaignId(Long campaignId);
}
