package com.practicket.community.domain.entity;

import com.practicket.client.domain.Client;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 대상을 FK 가 아니라 {@code targetType + targetId} 로 가리킨다 —
 * 어드민 신고함이 글과 댓글을 한 줄에 섞어 정렬하는 화면이라 테이블을 나누면 매번 합쳐야 한다.
 */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"target_type", "target_id", "reporter_ip"}))
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String reporterIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
