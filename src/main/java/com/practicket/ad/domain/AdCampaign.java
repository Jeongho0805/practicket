package com.practicket.ad.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ad_campaign")
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long advertiserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startAt;

    @Column(nullable = false)
    private LocalDate endAt;

    @Column(nullable = false)
    private Long amount;

    @Column(columnDefinition = "TEXT")
    private String linkUrl;

    @Column(nullable = false, unique = true)
    private String reportToken;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    public void delete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(Long advertiserId, String name, LocalDate startAt, LocalDate endAt,
                       Long amount, String linkUrl, String memo) {
        this.advertiserId = advertiserId;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.amount = amount;
        this.linkUrl = linkUrl;
        this.memo = memo;
    }

    public boolean isRunning(LocalDate today) {
        return !today.isBefore(startAt) && !today.isAfter(endAt);
    }

    public boolean isUpcoming(LocalDate today) {
        return today.isBefore(startAt);
    }

    public boolean isFinished(LocalDate today) {
        return today.isAfter(endAt);
    }
}
