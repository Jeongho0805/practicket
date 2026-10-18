package com.practicket.drill.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DrillCaptchaStatRepository extends JpaRepository<DrillCaptchaStat, Long> {

    boolean existsByClientId(Long clientId);

    Optional<DrillCaptchaStat> findByClientId(Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DrillCaptchaStat s where s.clientId = :clientId")
    Optional<DrillCaptchaStat> findByClientIdForUpdate(@Param("clientId") Long clientId);
}
