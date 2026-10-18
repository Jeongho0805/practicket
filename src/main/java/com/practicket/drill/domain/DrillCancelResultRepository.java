package com.practicket.drill.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrillCancelResultRepository extends JpaRepository<DrillCancelResult, Long> {

    long countByClientIdAndForm(Long clientId, DrillSeatForm form);

    List<DrillCancelResult> findTop10ByClientIdAndFormOrderByIdDesc(Long clientId, DrillSeatForm form);

    Optional<DrillCancelResult> findFirstByClientIdAndFormOrderByAverageMsAscIdAsc(Long clientId, DrillSeatForm form);

    Optional<DrillCancelResult> findFirstByClientIdAndFormAndMisclicksOrderByAverageMsAscIdAsc(Long clientId, DrillSeatForm form, int misclicks);

    Optional<DrillCancelResult> findFirstByClientIdAndFormAndMisclicksAndMissedTicketsOrderByAverageMsAscIdAsc(
            Long clientId, DrillSeatForm form, int misclicks, int missedTickets);
}
