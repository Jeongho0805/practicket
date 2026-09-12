package com.practicket.blog.dto;

import com.practicket.blog.domain.BlogPostSummary;

import java.time.format.DateTimeFormatter;

public record BlogPostCard(
        Long id,
        String title,
        String subtitle,
        String thumbnailImagePath,
        String dateLabel,
        String viewLabel,
        boolean hasViews
) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    public static BlogPostCard from(BlogPostSummary summary) {
        return new BlogPostCard(
                summary.getId(),
                summary.getTitle(),
                summary.getSubtitle(),
                summary.getThumbnailImagePath(),
                summary.getPublishedAt() == null ? "" : DATE.format(summary.getPublishedAt()),
                compact(summary.getViewCount()),
                summary.getViewCount() != null && summary.getViewCount() > 0
        );
    }

    /** 1000 → 1k, 1500 → 1.5k. 화면의 다른 카운트와 같은 표기다 */
    static String compact(Long count) {
        return compact(count == null ? 0L : count);
    }

    static String compact(long value) {
        if (value >= 1_000_000) {
            return trimZero(value / 1_000_000.0) + "m";
        }
        if (value >= 1_000) {
            return trimZero(value / 1_000.0) + "k";
        }
        return String.valueOf(value);
    }

    private static String trimZero(double value) {
        String formatted = String.format("%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
