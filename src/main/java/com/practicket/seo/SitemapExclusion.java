package com.practicket.seo;

import java.util.List;
import java.util.Map;

/**
 * 색인하지 않기로 한 경로와 그 이유. 이유를 코드에 남겨야 "빠뜨린 것"과 "뺀 것"이 구분된다.
 * 지금까지 sitemap 이 썩은 원인이 그 구분이 없어서였다.
 */
public final class SitemapExclusion {

    private static final Map<String, String> REASONS = Map.ofEntries(
            Map.entry("/rank", "/ticketing 으로 리다이렉트만 한다"),
            Map.entry("/reservation", "예매 연습 실행 화면"),
            Map.entry("/practice/i-ticket", "예매 연습 실행 화면"),
            Map.entry("/practice/i-ticket/intro", "예매 연습 실행 화면"),
            Map.entry("/notice/{id}", "점검 안내 수준이라 얇은 콘텐츠다"),
            Map.entry("/art/{id}", "제목 20자짜리가 수백 개면 사이트 평가만 깎인다"),
            Map.entry("/art/create", "입력 폼"),
            Map.entry("/art/edit/{id}", "입력 폼"),
            Map.entry("/community/write", "입력 폼"),
            Map.entry("/community/edit/{id}", "입력 폼"),
            Map.entry("/community/mine", "개인화 화면"),
            Map.entry("/community/{id}", "sitemap-community.xml 이 조건을 통과한 글만 따로 싣는다"),
            Map.entry("/blog/{id}", "sitemap.xml 이 템플릿을 훑어 직접 싣는다"),
            Map.entry("/ad/click/{bannerId}", "클릭 집계 후 광고주 주소로 넘기는 리다이렉트"),
            Map.entry("/ad/report/{token}", "토큰을 아는 사람만 여는 광고 리포트"),
            Map.entry("/ad/report/advertiser/{token}", "토큰을 아는 사람만 여는 광고주 리포트")
    );

    private SitemapExclusion() {
    }

    public static boolean contains(String path) {
        return REASONS.containsKey(path);
    }

    public static List<String> paths() {
        return REASONS.keySet().stream().sorted().toList();
    }

    public static String reasonOf(String path) {
        return REASONS.get(path);
    }
}
