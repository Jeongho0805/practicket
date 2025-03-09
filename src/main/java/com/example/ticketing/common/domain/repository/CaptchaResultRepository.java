package com.example.ticketing.common.domain.repository;

import com.example.ticketing.common.domain.entity.CaptchaResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptchaResultRepository extends JpaRepository<CaptchaResult, Long>, CaptchaResultRepositoryCustom  {}