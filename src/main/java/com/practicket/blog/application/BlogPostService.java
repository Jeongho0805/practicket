package com.practicket.blog.application;

import com.practicket.blog.domain.BlogPost;
import com.practicket.blog.domain.BlogPostRepository;
import com.practicket.blog.domain.BlogPostStatus;
import com.practicket.blog.domain.BlogPostSummary;
import com.practicket.blog.dto.BlogPostCard;
import com.practicket.blog.dto.BlogPostDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 목록·상세·sitemap 이 모두 이 하나를 거친다.
 * 공개 여부 판정이 여기 한 곳에만 있어야 어느 화면에서도 미발행 글이 새지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogPostService {

    private static final int NEXT_POST_COUNT = 3;

    private final BlogPostRepository blogPostRepository;

    public List<BlogPostCard> publishedCards() {
        return blogPostRepository.findByStatusOrderByPublishedAtDescIdDesc(BlogPostStatus.PUBLISHED)
                .stream()
                .map(BlogPostCard::from)
                .toList();
    }

    /** 조회수는 화면에 보이는 값이라, 방금 올린 만큼을 반영해 내려준다 */
    @Transactional
    public Optional<BlogPostDetail> readPublished(Long id) {
        return blogPostRepository.findByIdAndStatus(id, BlogPostStatus.PUBLISHED)
                .map(post -> {
                    blogPostRepository.incrementViewCount(post.getId());
                    return BlogPostDetail.of(post, nextCards(post), post.getViewCount() + 1);
                });
    }

    public List<Long> publishedIds() {
        return blogPostRepository.findByStatusOrderByPublishedAtDescIdDesc(BlogPostStatus.PUBLISHED)
                .stream()
                .map(BlogPostSummary::getId)
                .toList();
    }

    private List<BlogPostCard> nextCards(BlogPost post) {
        return blogPostRepository
                .findByStatusAndIdNotOrderByPublishedAtDescIdDesc(BlogPostStatus.PUBLISHED, post.getId())
                .stream()
                .limit(NEXT_POST_COUNT)
                .map(BlogPostCard::from)
                .toList();
    }
}
