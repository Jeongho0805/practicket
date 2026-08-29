package com.practicket.practice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "practice_result", indexes = {
        @Index(name = "idx_practice_result_type_started_at", columnList = "type, started_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PracticeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientKey;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PracticeType type;

    @Column(nullable = false)
    private LocalDateTime startedAt;

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
    private int queueInitialRank;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PracticeResult(String clientKey, String nickname, PracticeType type, LocalDateTime startedAt,
                          int totalDurationMs, int reactionTimeMs, int queueWaitMs,
                          int captchaMs, int seatSelectionMs, int queueInitialRank) {
        this.clientKey = clientKey;
        this.nickname = nickname;
        this.type = type;
        this.startedAt = startedAt;
        this.totalDurationMs = totalDurationMs;
        this.reactionTimeMs = reactionTimeMs;
        this.queueWaitMs = queueWaitMs;
        this.captchaMs = captchaMs;
        this.seatSelectionMs = seatSelectionMs;
        this.queueInitialRank = queueInitialRank;
    }
}
