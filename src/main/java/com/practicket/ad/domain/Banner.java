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
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private AdSlot slot;

    private Long campaignId;

    private String imagePath;

    private String pcImagePath;

    private String mobileImagePath;

    @Column(columnDefinition = "TEXT")
    private String linkUrl;

    private String advertiserName;

    private LocalDate startAt;

    private LocalDate endAt;

    private Boolean enabled;

    private String reportToken;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 캠페인에서 빼도 행은 남긴다. 노출·클릭 이력이 이 행을 가리키기 때문이다 */
    private LocalDateTime deletedAt;

    public void delete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
