package com.practicket.blog.domain;

import java.time.LocalDateTime;

/** 목록·sitemap 은 본문이 필요 없다. content 는 longtext 라 15건만 읽어도 무겁다 */
public interface BlogPostSummary {
    Long getId();
    String getTitle();
    String getSubtitle();
    String getThumbnailImagePath();
    LocalDateTime getPublishedAt();
    Long getViewCount();
}
