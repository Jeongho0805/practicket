package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdUnitRepository extends JpaRepository<AdUnit, Long> {

    List<AdUnit> findAllByOrderByNetworkAscNameAsc();
}
