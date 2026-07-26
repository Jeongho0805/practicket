package com.practicket.notice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 공지·업데이트 글. 홈 상단 5건과 /notice 전체 목록이 같은 데이터를 본다.
 *
 * 삭제는 커뮤니티 글(soft delete)과 달리 행을 실제로 지운다.
 * 운영자가 직접 쓰고 지우는 데이터라 보존 요구가 없고, 남겨두면 목록 쿼리만 복잡해진다.
 */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 상단 고정. 목록에서 최신순보다 먼저 나온다. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    /** 비공개면 어드민에만 보인다. 미리 써두고 나중에 여는 용도. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean published = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(NoticeType type, String title, String content, boolean pinned, boolean published) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
    }

    public void togglePublished() {
        this.published = !this.published;
    }
}
