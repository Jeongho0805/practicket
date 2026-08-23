package com.practicket.community.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagNormalizerTest {

    @Test
    @DisplayName("영문은 소문자로 통일한다 — #IVE 와 #ive 는 같은 태그다")
    void lowercasesEnglish() {
        assertThat(TagNormalizer.normalizeOne("IVE")).isEqualTo("ive");
    }

    @Test
    @DisplayName("공백과 특수문자는 지운다")
    void stripsSpacesAndSymbols() {
        assertThat(TagNormalizer.normalizeOne(" #세븐틴! ")).isEqualTo("세븐틴");
    }

    @Test
    @DisplayName("12자를 넘으면 자른다. 길다고 버리지 않는다")
    void cutsAtTwelveCharacters() {
        assertThat(TagNormalizer.normalizeOne("가나다라마바사아자차카타파하")).isEqualTo("가나다라마바사아자차카타");
    }

    @Test
    @DisplayName("남는 글자가 없으면 빈 문자열이다")
    void returnsEmptyWhenNothingRemains() {
        assertThat(TagNormalizer.normalizeOne("!!!")).isEmpty();
        assertThat(TagNormalizer.normalizeOne(null)).isEmpty();
    }

    @Test
    @DisplayName("한 글에 세 개까지만 남긴다")
    void keepsAtMostThreeTags() {
        List<String> tags = TagNormalizer.normalize(List.of("하나", "둘", "셋", "넷"));

        assertThat(tags).containsExactly("하나", "둘", "셋");
    }

    @Test
    @DisplayName("정규화 후 같아지는 태그는 하나로 합친다")
    void removesDuplicatesAfterNormalizing() {
        List<String> tags = TagNormalizer.normalize(List.of("IVE", "ive", " i v e "));

        assertThat(tags).containsExactly("ive");
    }

    @Test
    @DisplayName("입력 순서를 지킨다 — 첫 태그가 목록에서 대표로 보인다")
    void keepsInputOrder() {
        List<String> tags = TagNormalizer.normalize(List.of("후기", "세븐틴"));

        assertThat(tags).containsExactly("후기", "세븐틴");
    }

    @Test
    @DisplayName("빈 값과 null 은 걸러내고 남은 것만 센다")
    void skipsBlankTags() {
        List<String> tags = TagNormalizer.normalize(Arrays.asList("꿀팁", "", "  ", null, "후기"));

        assertThat(tags).containsExactly("꿀팁", "후기");
    }

    @Test
    @DisplayName("태그를 안 달면 빈 목록이다 — 기본 태그를 대신 붙이지 않는다")
    void returnsEmptyListWhenNoTags() {
        assertThat(TagNormalizer.normalize(null)).isEmpty();
        assertThat(TagNormalizer.normalize(List.of())).isEmpty();
    }
}
