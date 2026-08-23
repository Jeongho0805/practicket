package com.practicket.community.component;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * 목록 화면의 주소를 만든다. Thymeleaf 의 {@code @{...(keyword=...)}} 는 값이 null 이어도
 * {@code keyword=} 를 빈 채로 붙여서 쓸 수 없다.
 */
public final class CommunityListLinks {

    private static final String PATH = "/community";

    /** 주소에 붙이지 않는다 — 같은 목록이 정렬만 다른 두 주소로 갈리면 안 된다 */
    private static final String DEFAULT_SORT = "latest";

    private final String keyword;
    private final String tag;
    private final String sort;

    public CommunityListLinks(String keyword, String tag, String sort) {
        this.keyword = keyword;
        this.tag = tag;
        this.sort = sort;
    }

    /** 검색어·태그·정렬을 유지한 채 페이지만 바꾼다 */
    public String page(int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PATH);
        // 1페이지는 page 를 안 붙인다 — canonical 주소와 같은 모양이어야 한다
        if (page > 0) {
            builder.queryParam("page", page);
        }
        appendKeyword(builder);
        appendTag(builder);
        appendSort(builder, sort);
        return toUri(builder);
    }

    /** 검색어·태그는 유지하고 페이지는 1페이지로 되돌린다 */
    public String sort(String sortType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PATH);
        appendKeyword(builder);
        appendTag(builder);
        appendSort(builder, sortType);
        return toUri(builder);
    }

    /** 정렬만 유지한다. {@code tagName} 이 비면 태그 해제 */
    public String tag(String tagName) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PATH);
        if (hasText(tagName)) {
            builder.queryParam("tag", tagName);
        }
        appendSort(builder, sort);
        return toUri(builder);
    }

    private void appendKeyword(UriComponentsBuilder builder) {
        if (hasText(keyword)) {
            builder.queryParam("keyword", keyword);
        }
    }

    private void appendTag(UriComponentsBuilder builder) {
        if (hasText(tag)) {
            builder.queryParam("tag", tag);
        }
    }

    private void appendSort(UriComponentsBuilder builder, String sortType) {
        if (hasText(sortType) && !DEFAULT_SORT.equals(sortType)) {
            builder.queryParam("sort", sortType);
        }
    }

    private String toUri(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
