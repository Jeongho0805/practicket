package com.practicket.drill.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrillGrapeResultRepository extends JpaRepository<DrillGrapeResult, Long> {

    long countByClientIdAndForm(Long clientId, DrillSeatForm form);

    List<DrillGrapeResult> findTop10ByClientIdAndFormOrderByIdDesc(Long clientId, DrillSeatForm form);

    Optional<DrillGrapeResult> findFirstByClientIdAndFormOrderByAverageMsAscIdAsc(Long clientId, DrillSeatForm form);

    Optional<DrillGrapeResult> findFirstByClientIdAndFormAndMisclicksOrderByAverageMsAscIdAsc(Long clientId, DrillSeatForm form, int misclicks);
}
