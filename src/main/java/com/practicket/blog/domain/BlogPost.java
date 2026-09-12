package com.practicket.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기존 15건은 templates/blog/1~15.html 에서 옮겨왔다.
 * 색인된 URL 을 지키려고 id 를 1~15 로 고정해 넣었으므로, 그 구간의 id 는 바꾸지 않는다.
 */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정본 제목. h1 · 목록 카드 · &lt;title&gt; 이 모두 이 값에서 나온다 */
    @Column(nullable = false)
    private String title;

    /** 부제. 목록 카드 설명과 검색결과 요약도 이 값에서 나온다 */
    @Column(length = 500)
    private String subtitle;

    /** 목록 카드 썸네일. 상세 화면에는 그리지 않는다 */
    private String thumbnailImagePath;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BlogPostStatus status = BlogPostStatus.PUBLISHED;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    /** 미발행 글에는 발행 시각이 없다 */
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isPublished() {
        return status == BlogPostStatus.PUBLISHED;
    }

    public void update(String title, String subtitle,
                       String thumbnailImagePath, String content) {
        this.title = title;
        this.subtitle = subtitle;
        this.thumbnailImagePath = thumbnailImagePath;
        this.content = content;
    }

    /** 처음 공개할 때만 발행 시각을 찍는다 — 다시 올려도 목록 맨 위로 튀지 않게 한다 */
    public void publish() {
        this.status = BlogPostStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void unpublish() {
        this.status = BlogPostStatus.UNPUBLISHED;
    }
}
