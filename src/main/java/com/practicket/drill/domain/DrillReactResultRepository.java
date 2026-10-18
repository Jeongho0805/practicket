package com.practicket.drill.domain;

import com.practicket.drill.dto.DrillSummaryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrillReactResultRepository extends JpaRepository<DrillReactResult, Long> {

    long countByClientIdAndForm(Long clientId, DrillReactForm form);

    long countByClientId(Long clientId);

    List<DrillReactResult> findTop10ByClientIdAndFormOrderByIdDesc(Long clientId, DrillReactForm form);

    Optional<DrillReactResult> findFirstByClientIdAndFormOrderByReactionMsAscIdAsc(Long clientId, DrillReactForm form);

    @Query("""
            select new com.practicket.drill.dto.DrillSummaryResponse$Best(r.form, min(r.reactionMs))
            from DrillReactResult r
            where r.clientId = :clientId
            group by r.form
            """)
    List<DrillSummaryResponse.Best> findBests(@Param("clientId") Long clientId);
}
