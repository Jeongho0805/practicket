package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PracticeResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeResultRepository extends JpaRepository<PracticeResult, Long> {
}
