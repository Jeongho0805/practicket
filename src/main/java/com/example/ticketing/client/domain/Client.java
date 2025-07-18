package com.example.ticketing.client.domain;

import com.example.ticketing.captcha.domain.CaptchaResult;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private String device;

    @Column(nullable = false)
    private String referer;

    private String name;

    @Column(nullable = false)
    private Boolean banned;

    private String banReason;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<CaptchaResult> captchaResults = new ArrayList<>();
}
