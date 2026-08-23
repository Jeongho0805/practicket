package com.practicket.inquiry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    long countByClientTokenAndCreatedAtAfter(String clientToken, LocalDateTime after);
}
