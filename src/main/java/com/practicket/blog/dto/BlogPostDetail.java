package com.practicket.blog.dto;

import com.practicket.blog.domain.BlogPost;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record BlogPostDetail(
        Long id,
        String title,
        String subtitle,
        String content,
        String dateLabel,
        String viewLabel,
        boolean hasViews,
        List<BlogPostCard> next
) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    public static BlogPostDetail of(BlogPost post, List<BlogPostCard> next, long viewCount) {
        return new BlogPostDetail(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getContent(),
                post.getPublishedAt() == null ? "" : DATE.format(post.getPublishedAt()),
                BlogPostCard.compact(viewCount),
                viewCount > 0,
                next
        );
    }
}
