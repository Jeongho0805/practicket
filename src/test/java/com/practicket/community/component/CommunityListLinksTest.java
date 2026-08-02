package com.practicket.community.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityListLinksTest {

    private static CommunityListLinks noFilter() {
        return new CommunityListLinks(null, null, "latest");
    }

    @Nested
    @DisplayName("빈 값은 주소에 붙지 않는다 — 이 클래스를 만든 이유다")
    class OmitsBlankParams {

        @Test
        @DisplayName("아무 조건도 없으면 파라미터가 하나도 없다")
        void noParamsWhenNothingApplied() {
            assertThat(noFilter().page(0)).isEqualTo("/community");
        }

        @Test
        @DisplayName("빈 문자열도 없는 것으로 본다 — 폼이 빈 칸을 보내도 주소는 깨끗해야 한다")
        void treatsEmptyStringAsAbsent() {
            CommunityListLinks links = new CommunityListLinks("", "", "latest");

            assertThat(links.page(3)).isEqualTo("/community?page=3");
        }

        @Test
        @DisplayName("공백만 있는 검색어도 없는 것으로 본다")
        void treatsBlankKeywordAsAbsent() {
            assertThat(new CommunityListLinks("   ", null, "latest").page(0))
                    .isEqualTo("/community");
        }

        @Test
        @DisplayName("기본 정렬(latest)은 붙이지 않는다 — 같은 목록이 두 주소로 갈리면 안 된다")
        void omitsDefaultSort() {
            assertThat(new CommunityListLinks(null, "팁", "latest").tag("팁"))
                    .doesNotContain("sort");
        }
    }

    @Nested
    @DisplayName("페이징 링크는 보던 조건을 그대로 들고 간다")
    class PageLinks {

        @Test
        @DisplayName("1페이지는 page 를 붙이지 않는다 — canonical 이 가리키는 주소와 같아야 한다")
        void omitsPageZero() {
            assertThat(new CommunityListLinks(null, null, "view").page(0))
                    .isEqualTo("/community?sort=view");
        }

        @Test
        @DisplayName("검색어·태그·정렬을 모두 유지한다")
        void keepsEveryFilter() {
            CommunityListLinks links = new CommunityListLinks("대기열", "팁", "view");

            assertThat(links.page(27))
                    .startsWith("/community?page=27")
                    .contains("sort=view");
        }
    }

    @Nested
    @DisplayName("정렬 링크는 페이지를 되돌린다")
    class SortLinks {

        @Test
        @DisplayName("3페이지를 보다 정렬을 바꾸면 1페이지로 간다 — 그 3페이지는 이미 다른 목록이다")
        void resetsPage() {
            CommunityListLinks links = new CommunityListLinks(null, null, "latest");

            assertThat(links.sort("like")).isEqualTo("/community?sort=like");
        }

        @Test
        @DisplayName("걸려 있던 태그는 유지한다")
        void keepsTag() {
            CommunityListLinks links = new CommunityListLinks(null, "후기", "latest");

            assertThat(links.sort("comment")).contains("sort=comment");
        }
    }

    @Nested
    @DisplayName("태그 링크는 정렬만 유지한다")
    class TagLinks {

        @Test
        @DisplayName("태그를 고르면 검색어는 버린다 — 새로 보기 시작하는 것이다")
        void dropsKeyword() {
            CommunityListLinks links = new CommunityListLinks("대기열", null, "view");

            assertThat(links.tag("후기")).doesNotContain("keyword");
        }

        @Test
        @DisplayName("정렬은 들고 간다")
        void keepsSort() {
            assertThat(new CommunityListLinks(null, null, "view").tag("후기"))
                    .contains("sort=view");
        }

        @Test
        @DisplayName("null 을 주면 태그 해제(전체)다")
        void nullMeansAllTags() {
            assertThat(new CommunityListLinks(null, "후기", "latest").tag(null))
                    .isEqualTo("/community");
        }
    }

    @Test
    @DisplayName("한글은 인코딩해서 내보낸다. 안 하면 주소가 깨진다")
    void encodesKorean() {
        String link = new CommunityListLinks(null, null, "latest").tag("세븐틴");

        assertThat(link).isEqualTo("/community?tag=%EC%84%B8%EB%B8%90%ED%8B%B4");
    }

    @Test
    @DisplayName("검색어의 & 와 공백도 인코딩한다 — 안 하면 파라미터가 하나 더 생긴 것처럼 읽힌다")
    void encodesSpecialCharactersInKeyword() {
        String link = new CommunityListLinks("a&b c", null, "latest").page(0);

        assertThat(link).isEqualTo("/community?keyword=a%26b%20c");
    }
}
