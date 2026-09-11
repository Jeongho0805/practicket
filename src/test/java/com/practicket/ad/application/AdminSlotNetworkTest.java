package com.practicket.ad.application;

import com.practicket.ad.component.AdUnitResolver;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.exception.AdException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AdminSlotNetworkTest {
    private final AdSlotRepository slots = mock(AdSlotRepository.class);
    private final AdUnitRepository units = mock(AdUnitRepository.class);
    private final AdUnitResolver resolver = new AdUnitResolver();
    private final AdminSlotService service = new AdminSlotService(slots, units, resolver,
            mock(AdminCampaignService.class));
    private final AdUnit adsense = AdUnit.builder().id(1L).network("ADSENSE").unitId("123").build();
    private final AdUnit adfit = AdUnit.builder().id(2L).network("ADFIT").unitId("DAN-test")
            .width(300).height(250).build();

    @Test
    void changingNetworkClearsBothOverridesAndResolvesNewNetwork() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        given(units.findAll()).willReturn(List.of(adsense, adfit));

        service.changeFillNetwork(1L, "ADFIT");

        assertThat(slot.getPcAdUnitId()).isNull();
        assertThat(slot.getMobileAdUnitId()).isNull();
        assertThat(resolver.resolve(slot, true, List.of(adsense, adfit))).contains(adfit);
    }

    @Test
    void clearingNetworkClearsOverridesAndRendersNothing() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        service.changeFillNetwork(1L, "");
        assertThat(slot.getPcAdUnitId()).isNull();
        assertThat(slot.getMobileAdUnitId()).isNull();
        assertThat(resolver.resolve(slot, true, List.of(adsense))).isEmpty();
    }

    @Test
    void invalidLegacyOverrideNeverRendersOtherNetwork() {
        assertThat(resolver.resolve("ADFIT", 1L, 300, 250, List.of(adsense, adfit))).contains(adfit);
        assertThat(resolver.resolve("ADFIT", 1L, 320, 100, List.of(adsense, adfit))).isEmpty();
    }

    @Test
    void rejectForeignUnitBeforeMutatingEitherDevice() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        given(units.findById(2L)).willReturn(Optional.of(adfit));
        assertThatThrownBy(() -> service.changeUnits(1L, "ADSENSE", null, 2L))
                .isInstanceOf(AdException.class);
        assertThat(slot.getPcAdUnitId()).isEqualTo(1L);
        assertThat(slot.getMobileAdUnitId()).isEqualTo(1L);
    }

    @Test
    void rejectStaleNetworkAndUnknownNetwork() {
        given(slots.findById(1L)).willReturn(Optional.of(slot()));
        assertThatThrownBy(() -> service.changeUnits(1L, "ADFIT", null, null))
                .isInstanceOf(AdException.class);
        assertThatThrownBy(() -> service.changeFillNetwork(1L, "INVALID"))
                .isInstanceOf(AdException.class);
    }

    @Test
    void fullFormAlsoRejectsForeignAndDeletedUnits() {
        given(slots.findById(1L)).willReturn(Optional.of(slot()));
        given(units.findById(2L)).willReturn(Optional.of(adfit));
        AdminSlotService.SlotForm form = new AdminSlotService.SlotForm();
        form.setId(1L);
        form.setFillNetwork("ADSENSE");
        form.setPcAdUnitId(2L);
        assertThatThrownBy(() -> service.save(form)).isInstanceOf(AdException.class);
        form.setPcAdUnitId(99L);
        assertThatThrownBy(() -> service.save(form)).isInstanceOf(AdException.class);
        verify(slots, never()).save(any());
    }

    @Test
    void compatibleOverrideCanBeSavedAndReturnedToAutomatic() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        given(units.findById(1L)).willReturn(Optional.of(adsense));
        service.changeUnits(1L, "ADSENSE", 1L, null);
        assertThat(slot.getPcAdUnitId()).isEqualTo(1L);
        assertThat(slot.getMobileAdUnitId()).isNull();
        service.changeUnits(1L, "ADSENSE", null, null);
        assertThat(slot.getPcAdUnitId()).isNull();
    }

    private AdSlot slot() {
        return AdSlot.builder().id(1L).fillNetwork("ADSENSE")
                .pcWidth(300).pcHeight(600).mobileWidth(320).mobileHeight(100)
                .pcAdUnitId(1L).mobileAdUnitId(1L).build();
    }
}
