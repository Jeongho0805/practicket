package com.practicket.seo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sitemap 이 썩는 것을 막는 장치. 문서에 "새 페이지 만들면 sitemap 에 넣으세요"라고 적는 방식은
 * 이미 실패했다(정적 sitemap 시절 다섯 개가 누락돼 있었다).
 *
 * 그래서 화면 라우트를 전수 조사해 {@link SitemapPage} 나 {@link SitemapExclusion} 둘 중 하나에
 * 없으면 빌드를 깨뜨린다. 등록하지 않는 선택지 자체를 없애는 것이 목적이다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SitemapCoverageTest {

    /** 화면이 아닌 것들 — 여기까지 sitemap 후보로 볼 이유가 없다 */
    private static final List<String> NOT_A_PAGE_PREFIX = List.of(
            "/api", "/admin-hoya", "/error", "/actuator", "/sitemap");

    /** 스프링에 매핑 빈이 여럿이라(액추에이터 등) 이름으로 콕 집는다 */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private SitemapService sitemapService;

    @Test
    @DisplayName("모든 화면 라우트는 sitemap 에 싣거나, 이유를 적어 제외하거나 둘 중 하나여야 한다.")
    void everyPageIsEitherIndexedOrExcluded() {
        Set<String> unregistered = new TreeSet<>();

        handlerMapping.getHandlerMethods().forEach((info, method) -> {
            for (String path : pathsOf(info)) {
                if (isNotAPage(path) || !isGet(info)) {
                    continue;
                }
                if (!SitemapPage.paths().contains(path) && !SitemapExclusion.contains(path)) {
                    unregistered.add(path);
                }
            }
        });

        assertThat(unregistered)
                .withFailMessage("""
                        sitemap 에 등록되지 않은 화면이 있습니다: %s

                        둘 중 하나를 하세요.
                        1) 색인할 페이지면 SitemapPage 에 추가
                        2) 색인하지 않을 페이지면 SitemapExclusion 에 경로와 '이유'를 추가
                        """, unregistered)
                .isEmpty();
    }

    @Test
    @DisplayName("블로그 글은 파일이라 DB 조회가 안 된다 — 템플릿 개수만큼 sitemap 에 실려야 한다.")
    void blogPostsAreListedFromTemplates() {
        List<String> blogPaths = sitemapService.blogPaths();

        assertThat(blogPaths).isNotEmpty();
        assertThat(blogPaths).allMatch(path -> path.startsWith("/blog/"));
        assertThat(blogPaths).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("제외 목록에는 반드시 이유가 붙어 있어야 한다 — 빠뜨린 것과 뺀 것을 구분하기 위한 장치다.")
    void everyExclusionHasReason() {
        for (String path : SitemapExclusion.paths()) {
            assertThat(SitemapExclusion.reasonOf(path))
                    .withFailMessage("제외 사유가 비어 있습니다: %s", path)
                    .isNotBlank();
        }
    }

    private List<String> pathsOf(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatterns().stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.copyOf(info.getPatternValues());
    }

    private boolean isGet(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        return methods.isEmpty() || methods.contains(RequestMethod.GET);
    }

    private boolean isNotAPage(String path) {
        return NOT_A_PAGE_PREFIX.stream().anyMatch(path::startsWith);
    }
}
