package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    Optional<AdCampaign> findByReportToken(String reportToken);

    List<AdCampaign> findByAdvertiserIdAndDeletedAtIsNullOrderByStartAtDesc(Long advertiserId);

    List<AdCampaign> findAllByDeletedAtIsNullOrderByStartAtDesc();

    long countByDeletedAtIsNull();

    long countByAdvertiserId(Long advertiserId);
}
