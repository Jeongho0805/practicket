package com.practicket.community.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostContentRendererTest {

    @Test
    @DisplayName("스크립트 태그는 글자로만 남는다 — 실행되지 않는다")
    void escapesScriptTag() {
        String rendered = PostContentRenderer.render("<script>alert(document.cookie)</script>");

        assertThat(rendered).doesNotContain("<script>");
        assertThat(rendered).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("이미지 onerror 같은 우회도 막힌다")
    void escapesEventHandlerInjection() {
        String rendered = PostContentRenderer.render("<img src=x onerror=\"steal()\">");

        assertThat(rendered).doesNotContain("<img");
        assertThat(rendered).doesNotContain("onerror=\"");
    }

    @Test
    @DisplayName("URL 은 nofollow 링크가 된다")
    void linkifiesUrl() {
        String rendered = PostContentRenderer.render("예매처는 https://tickets.interpark.com 입니다");

        assertThat(rendered).contains("<a href=\"https://tickets.interpark.com\"");
        assertThat(rendered).contains("rel=\"nofollow noopener noreferrer\"");
        assertThat(rendered).contains("target=\"_blank\"");
    }

    @Test
    @DisplayName("직접 넣은 a 태그는 살아나지 않는다 — 글자로만 남는다")
    void doesNotResurrectTagsThroughLinkify() {
        String rendered = PostContentRenderer.render("<a href=\"javascript:steal()\">클릭</a>");

        // 위험한 건 실행되는 앵커가 만들어지는 것이다. 글자로 남은 "javascript:" 는 무해하다.
        assertThat(rendered).doesNotContain("<a href");
        assertThat(rendered).contains("&lt;a href=&quot;javascript:steal()&quot;&gt;");
    }

    @Test
    @DisplayName("http/https 가 아닌 스킴은 링크로 만들지 않는다")
    void linkifiesOnlyHttpSchemes() {
        assertThat(PostContentRenderer.render("javascript:alert(1)")).doesNotContain("<a href");
        assertThat(PostContentRenderer.render("data:text/html,<script>")).doesNotContain("<a href");
        assertThat(PostContentRenderer.render("file:///etc/passwd")).doesNotContain("<a href");
    }

    @Test
    @DisplayName("줄바꿈은 태그로 바꾸지 않고 그대로 둔다 — 표시는 CSS 가 한다")
    void keepsNewlinesAsIs() {
        String rendered = PostContentRenderer.render("첫 줄\n둘째 줄");

        assertThat(rendered).isEqualTo("첫 줄\n둘째 줄");
        assertThat(rendered).doesNotContain("<br");
    }

    @Test
    @DisplayName("빈 값은 빈 문자열")
    void handlesEmptyContent() {
        assertThat(PostContentRenderer.render(null)).isEmpty();
        assertThat(PostContentRenderer.render("")).isEmpty();
    }
}
