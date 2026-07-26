package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BannerStatDailyRepository extends JpaRepository<BannerStatDaily, Long> {

    Optional<BannerStatDaily> findByBannerIdAndStatDate(Long bannerId, LocalDate statDate);

    List<BannerStatDaily> findByBannerIdAndStatDateBetweenOrderByStatDate(
            Long bannerId, LocalDate from, LocalDate to);
}
