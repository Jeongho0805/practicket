package com.practicket.drill.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DrillCaptchaStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long clientId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String letters;

    @Column(nullable = false)
    private Integer imeCount;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static DrillCaptchaStat empty(Long clientId) {
        DrillCaptchaStat stat = new DrillCaptchaStat();
        stat.clientId = clientId;
        stat.letters = CaptchaLetterStats.empty().toJson();
        stat.imeCount = 0;
        return stat;
    }

    public CaptchaLetterStats letterStats() {
        return CaptchaLetterStats.fromJson(letters);
    }

    public void update(CaptchaLetterStats stats, int imeAdded) {
        this.letters = stats.toJson();
        this.imeCount += imeAdded;
    }
}
