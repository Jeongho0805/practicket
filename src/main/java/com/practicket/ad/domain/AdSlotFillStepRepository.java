package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdSlotFillStepRepository extends JpaRepository<AdSlotFillStep, Long> {

    List<AdSlotFillStep> findAllByOrderBySlotIdAscStepOrderAsc();

    List<AdSlotFillStep> findAllBySlotIdOrderByStepOrderAsc(Long slotId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AdSlotFillStep s WHERE s.slotId = :slotId")
    void deleteAllBySlotId(@Param("slotId") Long slotId);
}
