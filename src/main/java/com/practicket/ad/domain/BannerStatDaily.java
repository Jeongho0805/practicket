package com.practicket.ad.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "banner_stat_daily",
        uniqueConstraints = @UniqueConstraint(columnNames = {"banner_id", "stat_date"}))
public class BannerStatDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bannerId;

    private LocalDate statDate;

    @Builder.Default
    private long impressions = 0L;

    @Builder.Default
    private long clicks = 0L;

    public void addImpressions(long amount) {
        this.impressions += amount;
    }

    public void addClicks(long amount) {
        this.clicks += amount;
    }
}
