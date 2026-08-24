package com.practicket.practice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 한 사람의 한 기간 최고 기록. 쓰기는 전부 네이티브 upsert 로 하고 이 엔티티는 읽기에만 쓴다.
 * 설계 배경은 db/migration/V13__create_practice_best_result.sql 머리말.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "practice_best_result")
public class PracticeBestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PracticeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PeriodType periodType;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private String clientKey;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Long resultId;

    @Column(nullable = false)
    private int totalDurationMs;

    @Column(nullable = false)
    private int reactionTimeMs;

    @Column(nullable = false)
    private int queueWaitMs;

    @Column(nullable = false)
    private int captchaMs;

    @Column(nullable = false)
    private int seatSelectionMs;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
