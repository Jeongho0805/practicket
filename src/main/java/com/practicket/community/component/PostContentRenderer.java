package com.practicket.community.component;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 본문을 화면에 넣을 HTML 로 바꾼다.
 *
 * 에디터를 넣지 않는 이유가 XSS 다(Q4-3). 인증 토큰이 localStorage 에 있어서
 * 태그 허용 목록에 구멍이 하나만 나도 그 글을 읽은 사람들의 토큰이 털린다.
 * 그래서 여기서 하는 일은 딱 둘이다 — 전부 escape 하고, URL 만 링크로 만든다.
 *
 * 순서가 중요하다. **먼저 전부 escape 한 뒤** 링크를 만든다.
 * 반대로 하면 링크를 만드는 과정에서 넣은 태그까지 escape 되거나,
 * 사용자가 넣은 태그가 살아남는다.
 *
 * 줄바꿈은 &lt;br&gt; 로 바꾸지 않고 CSS(white-space: pre-wrap)에 맡긴다.
 * 태그를 덜 만들수록 실수할 자리도 줄어든다.
 */
public final class PostContentRenderer {

    /** escape 된 문자열에서 링크를 찾으므로 & 는 이미 &amp; 다. 패턴에 그대로 태워도 안전하다. */
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[\\w\\-./?=%#+~:@!$'*,;()\\[\\]&]+");

    private PostContentRenderer() {
    }

    public static String render(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }

        String escaped = HtmlUtils.htmlEscape(rawContent);

        Matcher matcher = URL_PATTERN.matcher(escaped);
        StringBuilder rendered = new StringBuilder();

        while (matcher.find()) {
            String url = matcher.group();
            // rel 에 nofollow 가 빠지면 스팸 링크의 SEO 숙주가 된다.
            String anchor = "<a href=\"" + url + "\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">"
                    + url + "</a>";
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(rendered);

        return rendered.toString();
    }
}
