package com.practicket.ad.component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 광고 지표에서 제외할 트래픽 판정. 노출·클릭이 같은 기준을 쓰도록 한 곳에 둔다.
 * (기준이 갈리면 CTR = 클릭/노출 이 왜곡된다.)
 *
 * 완벽한 봇 차단이 목적이 아니다. 크롤러·링크 미리보기·프리페치처럼
 * 수치를 몇 배로 튀게 만드는 것만 걸러내는 수준이면 충분하다.
 */
@Component
public class AdTrafficFilter {

    private static final Pattern BOT_UA = Pattern.compile(
            "bot|crawler|spider|crawling|slurp|bingpreview|headless|facebookexternalhit|"
                    + "kakaotalk-scrap|slackbot|twitterbot|discordbot|telegrambot|whatsapp|"
                    + "preview|monitor|curl|wget|python-requests|okhttp|java/",
            Pattern.CASE_INSENSITIVE);

    /** 지표에서 제외해야 하는 요청인가. */
    public boolean isExcluded(HttpServletRequest request) {
        return isBot(request.getHeader("User-Agent")) || isPrefetch(request);
    }

    public boolean isBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT_UA.matcher(userAgent).find();
    }

    /**
     * 브라우저·메신저가 링크를 미리 받아두는 요청. 사용자가 누른 게 아니므로 클릭이 아니다.
     * Chrome/Safari는 Sec-Purpose: prefetch, 구형은 Purpose: prefetch / X-Moz: prefetch 를 보낸다.
     */
    private boolean isPrefetch(HttpServletRequest request) {
        return hasPrefetchValue(request.getHeader("Sec-Purpose"))
                || hasPrefetchValue(request.getHeader("Purpose"))
                || hasPrefetchValue(request.getHeader("X-Moz"))
                || hasPrefetchValue(request.getHeader("X-Purpose"));
    }

    private boolean hasPrefetchValue(String headerValue) {
        if (headerValue == null) {
            return false;
        }
        String lower = headerValue.toLowerCase();
        return lower.contains("prefetch") || lower.contains("preview") || lower.contains("prerender");
    }
}
