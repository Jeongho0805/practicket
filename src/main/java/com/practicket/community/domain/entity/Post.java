package com.practicket.community.domain.entity;

import com.practicket.client.domain.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 삭제는 행을 지우지 않고 deletedAt 만 남긴다. 조건을 빠뜨리면 지운 글이 노출되므로
 * 개별 쿼리가 아니라 {@link SQLRestriction} 으로 한 곳에서 막는다.
 */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 작성 시점 스냅샷. Client 를 조인하면 닉네임을 바꿨을 때 과거 글까지 소급 변경된다 */
    @Column(nullable = false)
    private String nickname;

    /** 화면에는 앞 두 마디만 표시한다 */
    @Column(nullable = false)
    private String ip;

    /** 네 자리 숫자의 bcrypt 해시 */
    @Column(nullable = false)
    private String deletePasswordHash;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long commentCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean blinded = false;

    /** 상세 화면에 (수정됨) 만 띄운다. 수정 시각은 노출하지 않는다 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean edited = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.edited = true;
    }

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
