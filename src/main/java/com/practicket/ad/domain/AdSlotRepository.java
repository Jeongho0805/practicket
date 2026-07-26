package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdSlotRepository extends JpaRepository<AdSlot, Long> {

    Optional<AdSlot> findByCode(String code);

    List<AdSlot> findByEnabledTrue();
}
