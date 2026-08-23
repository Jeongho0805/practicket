package com.practicket.seo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    public String sitemap() {
        return sitemapService.staticSitemap();
    }

    /** 글 수가 계속 늘어 정적 sitemap 과 분리한다 */
    @GetMapping(value = "/sitemap-community.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    public String communitySitemap() {
        return sitemapService.communitySitemap();
    }
}
