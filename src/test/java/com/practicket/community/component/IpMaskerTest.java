package com.practicket.community.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpMaskerTest {

    @Test
    @DisplayName("IPv4 는 앞 두 마디만 남는다")
    void masksIpv4ToTwoSegments() {
        assertThat(IpMasker.maskToTwoSegments("118.235.13.7")).isEqualTo("118.235");
        assertThat(IpMasker.maskToTwoSegments("121.136.226.5")).isEqualTo("121.136");
    }

    @Test
    @DisplayName("IPv6 는 앞 두 그룹만 남는다")
    void masksIpv6ToTwoSegments() {
        assertThat(IpMasker.maskToTwoSegments("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
                .isEqualTo("2001:0db8");
    }

    @Test
    @DisplayName("값이 없으면 0.0 으로 표시한다 — 화면이 비지 않게")
    void fallsBackWhenIpIsMissing() {
        assertThat(IpMasker.maskToTwoSegments(null)).isEqualTo("0.0");
        assertThat(IpMasker.maskToTwoSegments("")).isEqualTo("0.0");
        assertThat(IpMasker.maskToTwoSegments("   ")).isEqualTo("0.0");
    }

    @Test
    @DisplayName("마디가 하나뿐이면 그대로 둔다")
    void keepsSingleSegmentAsIs() {
        assertThat(IpMasker.maskToTwoSegments("localhost")).isEqualTo("localhost");
    }
}
