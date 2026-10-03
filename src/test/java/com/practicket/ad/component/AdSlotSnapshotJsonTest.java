package com.practicket.ad.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 스냅샷은 레디스에 JSON 으로 들어간다. 필드를 늘린 뒤에도 값이 그대로 오가야 한다 */
class AdSlotSnapshotJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("extra 가 있는 단위는 JSON 을 오가며 값을 지킨다")
    void roundTripsUnitExtra() throws Exception {
        AdSlotSnapshot snapshot = new AdSlotSnapshot(List.of(new AdSlotSnapshot.Slot(
                "PC_RIGHT", 300, 600, null, null, "DISPLAY", "MOBSENSE",
                new AdSlotSnapshot.Unit("MOBSENSE", "1070053", 300, 600, "{\"frameCode\":\"90\"}"),
                null, List.of())));

        String json = objectMapper.writeValueAsString(snapshot);
        AdSlotSnapshot read = objectMapper.readValue(json, AdSlotSnapshot.class);

        assertThat(read.slots().get(0).pcUnit().extra()).isEqualTo("{\"frameCode\":\"90\"}");
    }

    @Test
    @DisplayName("노출 여부·재요청 간격·채움 순서도 JSON 을 오간다")
    void roundTripsExposureGapAndFallbacks() throws Exception {
        AdSlotSnapshot snapshot = new AdSlotSnapshot(List.of(new AdSlotSnapshot.Slot(
                "PC_LEFT", 300, 600, null, null, "DISPLAY", "ADSENSE",
                new AdSlotSnapshot.Unit("ADSENSE", "9697904962", null, null), null,
                List.of(new AdSlotSnapshot.Unit("MOBSENSE", "1070053", 300, 600),
                        new AdSlotSnapshot.Unit("COUPANG", "943782", null, null)),
                List.of(), List.of())),
                java.util.Map.of("ADSENSE", true, "COUPANG", false),
                java.util.Map.of("ADSENSE", 3));

        AdSlotSnapshot read = objectMapper.readValue(objectMapper.writeValueAsString(snapshot), AdSlotSnapshot.class);

        assertThat(read.refillGapOf("ADSENSE")).isEqualTo(3);
        assertThat(read.refillGapOf("COUPANG")).isNull();
        assertThat(read.slots().get(0).pcFallbacks()).extracting(AdSlotSnapshot.Unit::network)
                .containsExactly("MOBSENSE", "COUPANG");
        assertThat(read.isExposed("ADSENSE")).isTrue();
        assertThat(read.isExposed("COUPANG")).isFalse();
        assertThat(read.isExposed("MOBSENSE")).isFalse();
    }

    @Test
    @DisplayName("비어 있는 칸이 빠진 JSON 도 빈 목록으로 읽힌다")
    void readsJsonWithoutOptionalFields() throws Exception {
        String json = "{\"slots\":[{\"code\":\"PC_LEFT\",\"pcWidth\":300,\"pcHeight\":600,"
                + "\"mobileWidth\":null,\"mobileHeight\":null,\"format\":\"DISPLAY\",\"fillNetwork\":\"ADFIT\","
                + "\"pcUnit\":{\"network\":\"ADFIT\",\"unitId\":\"DAN-side\",\"width\":160,\"height\":600},"
                + "\"mobileUnit\":null,\"banners\":[]}]}";

        AdSlotSnapshot read = objectMapper.readValue(json, AdSlotSnapshot.class);

        assertThat(read.slots().get(0).pcUnit().unitId()).isEqualTo("DAN-side");
        assertThat(read.slots().get(0).pcUnit().extra()).isNull();
        assertThat(read.slots().get(0).pcFallbacks()).isEmpty();
        assertThat(read.refillGapOf("ADFIT")).isNull();
        assertThat(read.isExposed("ADFIT")).isFalse();
    }
}
