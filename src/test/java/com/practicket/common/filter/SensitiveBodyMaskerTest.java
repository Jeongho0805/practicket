package com.practicket.common.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveBodyMaskerTest {

    @Test
    @DisplayName("삭제 비밀번호를 가린다 — 필드명이 deletePassword 라 부분 일치로 잡아야 한다")
    void masksDeletePassword() {
        String body = "{\"title\":\"제목\",\"content\":\"본문\",\"deletePassword\":\"7391\"}";

        String masked = SensitiveBodyMasker.mask(body);

        assertThat(masked).doesNotContain("7391");
        assertThat(masked).contains("\"deletePassword\":\"***\"");
    }

    @Test
    @DisplayName("가리지 않아도 되는 값은 그대로 둔다 — 로그가 쓸모를 잃으면 안 된다")
    void keepsNonSensitiveFields() {
        String body = "{\"title\":\"제목\",\"content\":\"본문\",\"deletePassword\":\"7391\"}";

        String masked = SensitiveBodyMasker.mask(body);

        assertThat(masked).contains("\"title\":\"제목\"");
        assertThat(masked).contains("\"content\":\"본문\"");
    }

    @Test
    @DisplayName("문의자 이메일을 가린다")
    void masksEmail() {
        String body = "{\"email\":\"someone@example.com\",\"content\":\"문의합니다\"}";

        String masked = SensitiveBodyMasker.mask(body);

        assertThat(masked).doesNotContain("someone@example.com");
        assertThat(masked).contains("\"email\":\"***\"");
    }

    @Test
    @DisplayName("대소문자를 가리지 않는다")
    void masksRegardlessOfCase() {
        String body = "{\"reservationToken\":\"abc123\",\"userPWD\":\"0000\"}";

        String masked = SensitiveBodyMasker.mask(body);

        assertThat(masked).doesNotContain("abc123");
        assertThat(masked).doesNotContain("0000");
    }

    @Test
    @DisplayName("값 안에 이스케이프된 따옴표가 있어도 문자열 끝을 잘못 잡지 않는다")
    void handlesEscapedQuotesInsideValue() {
        String body = "{\"password\":\"a\\\"b\",\"title\":\"남아야 한다\"}";

        String masked = SensitiveBodyMasker.mask(body);

        assertThat(masked).doesNotContain("a\\\"b");
        assertThat(masked).contains("\"title\":\"남아야 한다\"");
    }

    @Test
    @DisplayName("빈 본문이나 null 은 그대로 돌려준다")
    void passesThroughEmptyBody() {
        assertThat(SensitiveBodyMasker.mask(null)).isNull();
        assertThat(SensitiveBodyMasker.mask("")).isEmpty();
    }
}
