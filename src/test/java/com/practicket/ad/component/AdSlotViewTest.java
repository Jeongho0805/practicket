package com.practicket.ad.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 렌더는 스냅샷만 본다. 노출 여부·재요청 간격도 거기서 읽고 DB 는 건드리지 않는다 */
class AdSlotViewTest {

    private final AdNetworkSettings networkSettings = new AdNetworkSettings("ca-pub-test", "tracking-test", "carousel");

    @Test
    void enablesOnlyNetworksExposedInSnapshot() {
        AdSlotSnapshotStore store = mock(AdSlotSnapshotStore.class);
        AdSlotSnapshot snapshot = new AdSlotSnapshot(List.of(
                slot("COUPANG_SLOT", new AdSlotSnapshot.Unit("COUPANG", "943782", null, null), null),
                slot("ADFIT_SLOT", new AdSlotSnapshot.Unit("ADFIT", "DAN-test", 300, 250), null)),
                Map.of("COUPANG", true, "ADSENSE", false, "ADFIT", false));
        given(store.get()).willReturn(snapshot);

        AdSlotView view = new AdSlotView(store, networkSettings);

        assertThat(view.render("COUPANG_SLOT").getPc().isFillEnabled()).isTrue();
        assertThat(view.render("ADFIT_SLOT").getPc().isFillEnabled()).isFalse();
        verify(store, times(1)).get();
    }

    @Test
    void carriesRefillGapAndFallbackWhenSlotHasOne() {
        AdSlotSnapshotStore store = mock(AdSlotSnapshotStore.class);
        given(store.get()).willReturn(new AdSlotSnapshot(List.of(
                new AdSlotSnapshot.Slot("PC_RIGHT", 300, 600, null, null, "디스플레이", "ADSENSE",
                        new AdSlotSnapshot.Unit("ADSENSE", "9697904962", null, null), null,
                        new AdSlotSnapshot.Unit("ADFIT", "DAN-side", 160, 600), null,
                        5, List.of())),
                Map.of("ADSENSE", true, "ADFIT", true)));

        AdFace pc = new AdSlotView(store, networkSettings).render("PC_RIGHT").getPc();

        assertThat(pc.getRefillGapMinutes()).isEqualTo(5);
        assertThat(pc.hasFallback()).isTrue();
        assertThat(pc.getFallback().getNetwork()).isEqualTo("ADFIT");
        assertThat(pc.getFallback().getSize()).isEqualTo("160x600");
        assertThat(pc.getFallback().getRefillGapMinutes()).isNull();
    }

    @Test
    void dropsFallbackWhenItsNetworkIsNotExposed() {
        AdSlotSnapshotStore store = mock(AdSlotSnapshotStore.class);
        given(store.get()).willReturn(new AdSlotSnapshot(List.of(
                new AdSlotSnapshot.Slot("PC_RIGHT", 300, 600, null, null, "디스플레이", "ADSENSE",
                        new AdSlotSnapshot.Unit("ADSENSE", "9697904962", null, null), null,
                        new AdSlotSnapshot.Unit("ADFIT", "DAN-side", 160, 600), null,
                        5, List.of())),
                Map.of("ADSENSE", true, "ADFIT", false)));

        AdFace pc = new AdSlotView(store, networkSettings).render("PC_RIGHT").getPc();

        assertThat(pc.getRefillGapMinutes()).isEqualTo(5);
        assertThat(pc.hasFallback()).isFalse();
    }

    @Test
    void keepsSlotSizeWhenNothingCanFillIt() {
        AdSlotSnapshotStore store = mock(AdSlotSnapshotStore.class);
        given(store.get()).willReturn(new AdSlotSnapshot(List.of(
                new AdSlotSnapshot.Slot("NO_UNIT", 300, 250, null, null,
                        "디스플레이", "ADFIT", null, null, null, null, List.of()))));

        AdSlotRender render = new AdSlotView(store, networkSettings).render("NO_UNIT");

        assertThat(render.getPc().isEmpty()).isTrue();
        assertThat(render.getPcClass()).isEqualTo("pc-empty");
        assertThat(render.getMobile().isEmpty()).isFalse();
        assertThat(render.getMobileClass()).isEqualTo("mo-none");
    }

    private AdSlotSnapshot.Slot slot(String code, AdSlotSnapshot.Unit unit, Integer gap) {
        return new AdSlotSnapshot.Slot(code, 300, 250, null, null,
                "디스플레이", unit.network(), unit, null, null, null, gap, List.of());
    }
}
