package com.practicket.client.domain;

import com.practicket.captcha.domain.CaptchaResult;
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

    @Column(nullable = false, unique = true)
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

    /**
     * 기간제 밴의 만료 시각(Q8). null 이면 영구정지.
     * banned=false 면 애초에 이 값을 보지 않는다 — {@link #isBanned(LocalDateTime)} 참고.
     */
    private LocalDateTime bannedUntil;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<CaptchaResult> captchaResults = new ArrayList<>();

    /**
     * 지금 이 순간 밴 상태인지. 글/댓글 저장 직전에 부른다.
     *
     * banned_until 이 지났다고 해서 별도 배치로 banned 를 false 로 되돌리지 않는다(Q8) —
     * 그 순간의 now 와 비교해서 그때그때 판단할 뿐이다. 그래서 이 메서드가 유일한 판단 지점이어야
     * 서비스마다 날짜 비교 로직이 흩어지지 않는다.
     */
    public boolean isBanned(LocalDateTime now) {
        if (!Boolean.TRUE.equals(banned)) {
            return false;
        }
        return bannedUntil == null || bannedUntil.isAfter(now);
    }

    /**
     * 밴 부여. 서비스가 banned/bannedUntil 필드를 직접 세팅하지 않고 반드시 이 메서드를 거치게 한다 —
     * 필드를 흩어서 세팅하면 나중에 "밴 해제 시 banReason 을 안 지운" 같은 반쪽짜리 상태가 생긴다.
     * until 이 null 이면 영구정지다(Q8 — 도배 1일 / 반복 7일 / 악질 영구).
     */
    public void ban(LocalDateTime until, String reason) {
        this.banned = true;
        this.bannedUntil = until;
        this.banReason = reason;
    }

    /**
     * 밴 해제. 밴은 최후 수단이라 잘못 눌렀을 때 되돌릴 방법이 반드시 있어야 한다.
     */
    public void unban() {
        this.banned = false;
        this.bannedUntil = null;
        this.banReason = null;
    }
}
