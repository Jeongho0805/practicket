package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    Optional<Banner> findByReportToken(String reportToken);

    List<Banner> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM Banner b WHERE b.enabled = true AND b.slot.enabled = true " +
            "AND b.slot.code = :code AND b.startAt <= :today AND b.endAt >= :today")
    List<Banner> findActiveBySlotCode(@Param("code") String code, @Param("today") LocalDate today);
}
