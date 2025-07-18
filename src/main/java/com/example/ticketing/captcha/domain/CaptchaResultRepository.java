package com.example.ticketing.captcha.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptchaResultRepository extends JpaRepository<CaptchaResult, Long>, CaptchaResultRepositoryCustom  {}