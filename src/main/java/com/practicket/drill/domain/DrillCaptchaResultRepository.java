package com.practicket.drill.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrillCaptchaResultRepository extends JpaRepository<DrillCaptchaResult, Long> {

    long countByClientId(Long clientId);

    @Query("""
            select new com.practicket.drill.domain.DrillCaptchaResultRepository$Row(r.averageMs, r.createdAt)
            from DrillCaptchaResult r
            where r.clientId = :clientId
            order by r.id desc
            limit 10
            """)
    List<Row> findRecent(@Param("clientId") Long clientId);

    @Query("""
            select new com.practicket.drill.domain.DrillCaptchaResultRepository$Row(r.averageMs, r.createdAt)
            from DrillCaptchaResult r
            where r.clientId = :clientId
            order by r.averageMs asc, r.id asc
            limit 1
            """)
    Optional<Row> findBest(@Param("clientId") Long clientId);

    record Row(int averageMs, java.time.LocalDateTime createdAt) {
    }
}
