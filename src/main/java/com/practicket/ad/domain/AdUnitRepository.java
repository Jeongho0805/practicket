package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdUnitRepository extends JpaRepository<AdUnit, Long> {

    List<AdUnit> findByNetwork(String network);

    List<AdUnit> findAllByOrderByNetworkAscNameAsc();

    @Query("SELECT u FROM AdUnit u WHERE u.network = :network AND u.isDefault = true")
    Optional<AdUnit> findDefaultByNetwork(@Param("network") String network);
}
