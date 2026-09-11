package com.practicket.ad.component;

import com.practicket.ad.application.AdNetworkExposureService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AdSlotViewTest {

    @Test
    void enablesOnlyNetworksAllowedInCurrentEnvironment() {
        AdSlotSnapshotStore store = mock(AdSlotSnapshotStore.class);
        AdNetworkExposureService exposureService = mock(AdNetworkExposureService.class);
        AdNetworkSettings networkSettings = new AdNetworkSettings("ca-pub-test", "tracking-test", "carousel");
        AdSlotSnapshot snapshot = new AdSlotSnapshot(List.of(
                slot("COUPANG_SLOT", new AdSlotSnapshot.Unit("COUPANG", "943782", null, null)),
                slot("ADFIT_SLOT", new AdSlotSnapshot.Unit("ADFIT", "DAN-test", 300, 250))));
        given(store.get()).willReturn(snapshot);
        given(exposureService.currentExposure()).willReturn(Map.of(
                "COUPANG", true,
                "ADSENSE", false,
                "ADFIT", false));

        AdSlotView view = new AdSlotView(store, networkSettings, exposureService);

        assertThat(view.render("COUPANG_SLOT").getPc().isFillEnabled()).isTrue();
        assertThat(view.render("ADFIT_SLOT").getPc().isFillEnabled()).isFalse();
        verify(exposureService, times(1)).currentExposure();
    }

    private AdSlotSnapshot.Slot slot(String code, AdSlotSnapshot.Unit unit) {
        return new AdSlotSnapshot.Slot(code, 300, 250, null, null,
                "디스플레이", unit.network(), unit, null, List.of());
    }
}
