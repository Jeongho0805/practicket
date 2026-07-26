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
 * 커뮤니티 글. 설계는 docs/community-system.md.
 *
 * 삭제는 행을 지우지 않고 deletedAt 에 시각을 남긴다(Q9).
 * 조회할 때 조건을 빠뜨리면 지운 글이 조용히 노출되므로,
 * 개별 쿼리에 맡기지 않고 {@link SQLRestriction} 으로 엔티티 한 곳에서 막는다.
 * 어드민이 지운 글을 봐야 할 때(9단계)는 이 제약을 우회하는 별도 조회를 만든다.
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

    /**
     * 작성 시점의 닉네임. Client 를 조인하지 않는다 —
     * 닉네임을 바꾸면 과거 글의 작성자가 전부 소급 변경되기 때문이다.
     */
    @Column(nullable = false)
    private String nickname;

    /** 작성 시점의 IP. 화면에는 앞 두 마디만 표시한다. */
    @Column(nullable = false)
    private String ip;

    /** 네 자리 숫자의 bcrypt 해시. 평문은 저장하지 않는다. */
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

    /** 수정되면 true. 상세 화면에 (수정됨) 만 띄우고 수정 시각은 노출하지 않는다. */
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
}
