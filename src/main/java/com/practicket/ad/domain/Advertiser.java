package com.practicket.ad.domain;

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
@Table(name = "advertiser")
public class Advertiser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String company;

    private String manager;

    private String phone;

    private String email;

    private String bizNo;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(String name, String company, String manager,
                       String phone, String email, String bizNo, String memo) {
        this.name = name;
        this.company = company;
        this.manager = manager;
        this.phone = phone;
        this.email = email;
        this.bizNo = bizNo;
        this.memo = memo;
    }
}
