package com.practicket.community.domain.entity;

import com.practicket.client.domain.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 대댓글·수정 기능은 없다. 대상 표시는 본문의 {@code >} 인용 한 줄이 맡는다 */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 작성 시점 스냅샷. 글과 같은 이유로 Client 를 조인하지 않는다 */
    @Column(nullable = false)
    private String nickname;

    /** 화면에는 앞 두 마디만 표시한다 */
    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean blinded = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isWrittenBy(Long clientId) {
        return clientId != null && this.client.getId().equals(clientId);
    }

    /** 서로 다른 IP 3개가 신고하면 화면에서 숨긴다 */
    public void blind() {
        this.blinded = true;
    }

    /** 어드민 모더레이션이 오판을 되돌릴 때 쓴다 */
    public void unblind() {
        this.blinded = false;
    }
}
