package com.practicket.client.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 밴 부여/해제. 설계는 docs/community-system.md Q8 — 기간제(도배 1일/반복 7일), until이 null이면 영구정지.
 */
class ClientTest {

    @Test
    @DisplayName("ban()은 banned를 true로 만들고 until·사유를 그대로 담는다.")
    void ban_setsFields() {
        Client client = client();
        LocalDateTime until = LocalDateTime.of(2026, 7, 27, 0, 0);

        client.ban(until, "도배");

        assertThat(client.getBanned()).isTrue();
        assertThat(client.getBannedUntil()).isEqualTo(until);
        assertThat(client.getBanReason()).isEqualTo("도배");
    }

    @Test
    @DisplayName("until이 null이면 영구정지 — isBanned는 시각과 무관하게 항상 true다.")
    void ban_nullUntil_isPermanent() {
        Client client = client();

        client.ban(null, "악질 도배");

        assertThat(client.isBanned(LocalDateTime.now())).isTrue();
        assertThat(client.isBanned(LocalDateTime.now().plusYears(10))).isTrue();
    }

    @Test
    @DisplayName("기간제 밴은 until이 지나면 isBanned가 false로 판정된다 — 별도 배치가 필요 없다.")
    void ban_expires() {
        Client client = client();
        LocalDateTime now = LocalDateTime.of(2026, 7, 26, 12, 0);
        client.ban(now.plusDays(1), "도배");

        assertThat(client.isBanned(now)).isTrue();
        assertThat(client.isBanned(now.plusDays(1).plusMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("unban()은 banned·bannedUntil·banReason을 모두 되돌린다.")
    void unban_clearsAllFields() {
        Client client = client();
        client.ban(LocalDateTime.now().plusDays(7), "반복 도배");

        client.unban();

        assertThat(client.getBanned()).isFalse();
        assertThat(client.getBannedUntil()).isNull();
        assertThat(client.getBanReason()).isNull();
        assertThat(client.isBanned(LocalDateTime.now())).isFalse();
    }

    private Client client() {
        return Client.builder()
                .id(1L).token("tok").ip("1.2.3.4").device("web").referer("-")
                .banned(false).build();
    }
}
