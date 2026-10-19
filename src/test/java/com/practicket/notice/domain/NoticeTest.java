package com.practicket.notice.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 12, 0);

    private static Notice createdAt(LocalDateTime at) {
        return Notice.builder().type(NoticeType.NOTICE).title("t").content("c").createdAt(at).build();
    }

    @Test
    void 올린_지_30일_안이면_새_글이다() {
        assertThat(createdAt(NOW.minusDays(1)).isFreshAt(NOW)).isTrue();
        assertThat(createdAt(NOW.minusDays(30).plusMinutes(1)).isFreshAt(NOW)).isTrue();
    }

    @Test
    void 올린_지_30일이_지나면_새_글이_아니다() {
        assertThat(createdAt(NOW.minusDays(30)).isFreshAt(NOW)).isFalse();
        assertThat(createdAt(NOW.minusDays(38)).isFreshAt(NOW)).isFalse();
    }
}
