package com.practicket.drill.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrillReactResultRepository extends JpaRepository<DrillReactResult, Long> {

    long countByClientIdAndForm(Long clientId, DrillReactForm form);

    List<DrillReactResult> findTop10ByClientIdAndFormOrderByIdDesc(Long clientId, DrillReactForm form);

    Optional<DrillReactResult> findFirstByClientIdAndFormOrderByReactionMsAscIdAsc(Long clientId, DrillReactForm form);
}
