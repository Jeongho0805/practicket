package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdUnitRepository extends JpaRepository<AdUnit, Long> {

    List<AdUnit> findAllByOrderByNetworkAscNameAsc();

    Optional<AdUnit> findByNetworkAndUnitId(String network, String unitId);
}
