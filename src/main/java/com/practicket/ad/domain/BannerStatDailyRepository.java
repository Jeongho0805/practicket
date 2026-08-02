package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BannerStatDailyRepository extends JpaRepository<BannerStatDaily, Long> {

    Optional<BannerStatDaily> findByBannerIdAndStatDate(Long bannerId, LocalDate statDate);

    List<BannerStatDaily> findByBannerIdAndStatDateBetweenOrderByStatDate(
            Long bannerId, LocalDate from, LocalDate to);

    /** 배너 목록의 누적 성과 컬럼용 — 전 기간 합산. */
    @Query("SELECT s.bannerId AS bannerId, SUM(s.impressions) AS impressions, SUM(s.clicks) AS clicks " +
            "FROM BannerStatDaily s GROUP BY s.bannerId")
    List<BannerStatSum> sumGroupByBanner();

    /** 대시보드 상위 캠페인용 — 지정 기간 합산. */
    @Query("SELECT s.bannerId AS bannerId, SUM(s.impressions) AS impressions, SUM(s.clicks) AS clicks " +
            "FROM BannerStatDaily s WHERE s.statDate BETWEEN :from AND :to GROUP BY s.bannerId")
    List<BannerStatSum> sumGroupByBannerBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 광고주 통합 리포트용 — 지정한 배너들만 날짜별로 합산. */
    @Query("SELECT s.statDate AS statDate, SUM(s.impressions) AS impressions, SUM(s.clicks) AS clicks " +
            "FROM BannerStatDaily s WHERE s.bannerId IN :bannerIds AND s.statDate BETWEEN :from AND :to " +
            "GROUP BY s.statDate ORDER BY s.statDate")
    List<DailyStatSum> sumGroupByDateForBanners(@Param("bannerIds") List<Long> bannerIds,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    /** 대시보드 추이 차트·KPI용 — 전 배너를 날짜별로 합산. */
    @Query("SELECT s.statDate AS statDate, SUM(s.impressions) AS impressions, SUM(s.clicks) AS clicks " +
            "FROM BannerStatDaily s WHERE s.statDate BETWEEN :from AND :to " +
            "GROUP BY s.statDate ORDER BY s.statDate")
    List<DailyStatSum> sumGroupByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
