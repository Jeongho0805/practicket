package com.practicket.ad.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 광고주 통합 리포트 토큰은 저장되지 않고 이름에서 매번 계산된다.
 * 따라서 "같은 이름 → 같은 토큰"과 "다른 이름 → 통과 불가"가 보안 경계 그 자체다.
 */
class AdvertiserReportTokenTest {

    private static final String SECRET = "test-secret-0123456789abcdef";

    private final AdvertiserReportToken token = new AdvertiserReportToken(SECRET);

    @Test
    @DisplayName("같은 광고주명이면 항상 같은 토큰이 나온다 — 광고주가 링크를 북마크할 수 있어야 한다.")
    void sameNameProducesSameToken() {
        String first = token.issue("모두컴퍼니");
        String second = token.issue("모두컴퍼니");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("이름이 다르면 토큰도 다르다 — 표기가 한 글자만 달라도 갈라진다.")
    void differentNamesProduceDifferentTokens() {
        List<String> names = List.of("모두컴퍼니", "모두 컴퍼니", "(주)모두컴퍼니", "티켓링크", "");

        Set<String> tokens = new HashSet<>();
        for (String name : names) {
            tokens.add(token.issue(name));
        }

        assertThat(tokens).hasSameSizeAs(names);
    }

    @Test
    @DisplayName("자기 토큰으로만 통과한다.")
    void matchesOnlyItsOwnName() {
        String issued = token.issue("모두컴퍼니");

        assertThat(token.matches("모두컴퍼니", issued)).isTrue();
        assertThat(token.matches("티켓링크", issued)).isFalse();
        assertThat(token.matches("모두 컴퍼니", issued)).isFalse();
    }

    @Test
    @DisplayName("엉터리 토큰은 통과하지 못한다 — 길이가 달라도 예외 없이 false여야 한다.")
    void rejectsMalformedToken() {
        assertThat(token.matches("모두컴퍼니", "deadbeef")).isFalse();
        assertThat(token.matches("모두컴퍼니", "")).isFalse();
        assertThat(token.matches("모두컴퍼니", "a".repeat(200))).isFalse();
    }

    @Test
    @DisplayName("시크릿이 다르면 같은 이름이라도 다른 토큰이 나온다 — 유출된 토큰이 다른 환경에서 통하지 않는다.")
    void tokenDependsOnSecret() {
        AdvertiserReportToken other = new AdvertiserReportToken("another-secret-value");

        assertThat(other.issue("모두컴퍼니")).isNotEqualTo(token.issue("모두컴퍼니"));
    }

    @Test
    @DisplayName("토큰은 URL에 그대로 실을 수 있는 짧은 문자열이다.")
    void tokenIsUrlSafeAndShort() {
        String issued = token.issue("모두컴퍼니 & 파트너 / 2026");

        assertThat(issued)
                .hasSize(22)                      // 128비트 base64url, 패딩 없음
                .matches("[A-Za-z0-9_-]+");       // 인코딩 없이 경로에 넣을 수 있어야 한다
    }
}
