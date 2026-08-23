package com.practicket.seo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * sitemap 에 올리는 정적 페이지. 새 페이지를 만들면 여기나 {@link SitemapExclusion} 둘 중 하나에
 * 반드시 등록해야 한다 — 아니면 SitemapCoverageTest 가 빌드를 깨뜨린다.
 *
 * changefreq·priority 는 두지 않는다. 구글이 무시하는 값이라 유지 비용만 든다.
 */
@Getter
@RequiredArgsConstructor
public enum SitemapPage {

    HOME("/"),
    TICKETING("/ticketing"),
    PRACTICE("/practice"),
    I_TICKET_INTRO("/practice/i-ticket/intro"),
    N_TICKET_INTRO("/practice/n-ticket/intro"),
    M_TICKET_INTRO("/practice/m-ticket/intro"),
    ART("/art"),
    COMMUNITY("/community"),
    BLOG("/blog"),
    SECURITY("/security"),
    ADVERTISE("/advertise"),
    NOTICE("/notice"),
    TERMS("/terms"),
    PRIVACY("/privacy");

    private final String path;

    public static List<String> paths() {
        return Arrays.stream(values()).map(SitemapPage::getPath).toList();
    }
}
