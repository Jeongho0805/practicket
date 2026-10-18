package com.practicket.drill.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DrillGrapeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DrillSeatForm form;

    @Column(nullable = false)
    private Integer averageMs;

    @Column(nullable = false)
    private Integer totalMs;

    @Column(nullable = false)
    private Integer seatCount;

    @Column(nullable = false)
    private Integer misclicks;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rounds;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
