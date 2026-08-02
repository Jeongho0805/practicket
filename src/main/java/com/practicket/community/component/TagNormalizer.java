package com.practicket.community.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 태그 정규화의 정본. 한글·영문·숫자만 남기고 영문은 소문자로, 12자까지, 3개까지, 중복 제거.
 * 안 하면 {@code #IVE}·{@code 아이브 }·{@code 아이브!} 가 전부 다른 태그가 된다.
 */
public final class TagNormalizer {

    /** 필수가 아니라 0개도 정상이다 */
    public static final int MAX_TAGS_PER_POST = 3;

    private static final int MAX_LENGTH = 12;

    /** 나머지(공백·#·이모지·구두점)는 전부 지운다 */
    private static final String ALLOWED_PATTERN = "[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]";

    private TagNormalizer() {
    }

    /** 넘치는 태그는 에러가 아니라 잘라낸다 — 태그가 글 작성을 막을 이유는 없다 */
    public static List<String> normalize(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }

        // 입력 순서를 유지한다. 첫 태그가 목록의 대표 태그다
        Set<String> normalized = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            String tag = normalizeOne(rawTag);
            if (!tag.isEmpty()) {
                normalized.add(tag);
            }
            if (normalized.size() == MAX_TAGS_PER_POST) {
                break;
            }
        }

        return new ArrayList<>(normalized);
    }

    /** 태그 필터(`?tag=…`)로 들어온 값을 저장된 값과 맞출 때 쓴다 */
    public static String normalizeOne(String rawTag) {
        if (rawTag == null) {
            return "";
        }

        String tag = rawTag.replaceAll(ALLOWED_PATTERN, "").toLowerCase();
        return tag.length() > MAX_LENGTH ? tag.substring(0, MAX_LENGTH) : tag;
    }
}
