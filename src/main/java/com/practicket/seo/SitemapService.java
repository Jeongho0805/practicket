package com.practicket.seo;

import com.practicket.blog.application.BlogPostService;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.notice.domain.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * sitemap 을 코드가 만든다. 정적 XML 이던 시절엔 새 페이지를 만들 때마다 손으로 넣어야 해서
 * 다섯 개가 누락돼 있었고 lastmod 는 1년 반 멈춰 있었다.
 *
 * 주소는 canonical 태그와 같은 값을 써야 한다 — 둘이 다르면 구글이 어느 쪽을 정본으로 볼지 흔들린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitemapService {

    private static final String SITE = "https://practicket.com";

    /** 반응 없는 짧은 글은 싣지 않는다 */
    private static final int MIN_CONTENT_LENGTH = 200;
    private static final int COMMUNITY_LIMIT = 5000;

    private static final String COMMUNITY_CACHE_KEY = "sitemap:community";
    private static final long COMMUNITY_CACHE_TTL_MINUTES = 60L;

    private static final DateTimeFormatter LASTMOD = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BlogPostService blogPostService;
    private final PostRepository postRepository;
    private final NoticeRepository noticeRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public String staticSitemap() {
        List<String> urls = new ArrayList<>();

        for (SitemapPage page : SitemapPage.values()) {
            urls.add(url(page.getPath(), page == SitemapPage.NOTICE ? latestNoticeDate() : null));
        }
        for (String path : blogPaths()) {
            urls.add(url(path, null));
        }

        return wrap(urls);
    }

    /**
     * 글이 늘수록 매 요청 조회가 부담이라 한 시간 캐시한다.
     * 크롤러는 자주 오지 않으므로 한 시간 늦게 반영돼도 문제되지 않는다.
     */
    public String communitySitemap() {
        String cached = stringRedisTemplate.opsForValue().get(COMMUNITY_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        List<Post> posts = postRepository.findForSitemap(MIN_CONTENT_LENGTH, COMMUNITY_LIMIT);
        List<String> urls = posts.stream()
                .map(post -> url("/community/" + post.getId(), post.getUpdatedAt()))
                .toList();

        String xml = wrap(urls);
        stringRedisTemplate.opsForValue().set(COMMUNITY_CACHE_KEY, xml, COMMUNITY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return xml;
    }

    /** 공개된 글만 싣는다. 미발행 글이 여기 새면 상세는 목록으로 돌리는데 색인은 되는 상태가 된다 */
    List<String> blogPaths() {
        return blogPostService.publishedIds().stream()
                .map(id -> "/blog/" + id)
                .toList();
    }

    private LocalDateTime latestNoticeDate() {
        return noticeRepository.findTopByPublishedTrueOrderByCreatedAtDesc()
                .map(com.practicket.notice.domain.Notice::getCreatedAt)
                .orElse(null);
    }

    private String url(String path, LocalDateTime lastModified) {
        StringBuilder sb = new StringBuilder("    <url>\n        <loc>")
                .append(SITE).append(path).append("</loc>\n");
        if (lastModified != null) {
            sb.append("        <lastmod>").append(lastModified.format(LASTMOD)).append("</lastmod>\n");
        }
        return sb.append("    </url>").toString();
    }

    private String wrap(List<String> urls) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """
                + String.join("\n", urls)
                + "\n</urlset>\n";
    }
}
