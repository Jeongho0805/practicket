package com.practicket.community.component;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 본문을 화면에 넣을 HTML 로 바꾼다. 전부 escape 한 뒤 URL 만 링크로 만든다. */
public final class PostContentRenderer {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[\\w\\-./?=%#+~:@!$'*,;()\\[\\]&]+");

    /** escape 된 뒤에 찾으므로 {@code >} 가 아니라 {@code &gt;} 를 본다 */
    private static final Pattern QUOTE_LINE_PATTERN = Pattern.compile("(?m)^&gt;.*$");

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
            // nofollow 가 빠지면 스팸 링크의 SEO 숙주가 된다
            String anchor = "<a href=\"" + url + "\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">"
                    + url + "</a>";
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(rendered);

        return rendered.toString();
    }

    /** 댓글용. 인용 줄 감싸기는 반드시 {@link #render} 뒤에 한다 — 앞에서 하면 사용자가 넣은 태그와 구분이 안 된다 */
    public static String renderComment(String rawContent) {
        String rendered = render(rawContent);
        if (rendered.isEmpty()) {
            return rendered;
        }

        Matcher matcher = QUOTE_LINE_PATTERN.matcher(rendered);
        StringBuilder quoted = new StringBuilder();

        while (matcher.find()) {
            String replacement = "<span class=\"comment-quote\">" + matcher.group() + "</span>";
            matcher.appendReplacement(quoted, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(quoted);

        return quoted.toString();
    }
}
