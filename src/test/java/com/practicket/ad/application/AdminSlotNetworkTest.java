package com.practicket.ad.application;

import com.practicket.ad.component.AdUnitResolver;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotFillStep;
import com.practicket.ad.domain.AdSlotFillStepRepository;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.exception.AdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AdminSlotNetworkTest {
    private final AdSlotRepository slots = mock(AdSlotRepository.class);
    private final AdSlotFillStepRepository steps = mock(AdSlotFillStepRepository.class);
    private final AdUnitRepository units = mock(AdUnitRepository.class);
    private final AdUnitResolver resolver = new AdUnitResolver();
    private final AdminSlotService service = new AdminSlotService(slots, steps, units, resolver,
            mock(AdminCampaignService.class));
    private final AdUnit adsense = AdUnit.builder().id(1L).network("ADSENSE").unitId("123").build();
    private final AdUnit adfit = AdUnit.builder().id(2L).network("ADFIT").unitId("DAN-test")
            .width(300).height(250).build();
    private final AdUnit coupang = AdUnit.builder().id(3L).network("COUPANG").unitId("943782").build();

    private final List<AdSlotFillStep> stored = new ArrayList<>();

    @BeforeEach
    void fakeStepTable() {
        given(steps.findAllBySlotIdOrderByStepOrderAsc(anyLong())).willAnswer(invocation -> List.copyOf(stored));
        doAnswer(invocation -> {
            stored.clear();
            return null;
        }).when(steps).deleteAllBySlotId(anyLong());
        given(steps.save(any())).willAnswer(invocation -> {
            stored.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
    }

    @Test
    void changingNetworkClearsBothOverridesAndResolvesNewNetwork() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));

        service.changeStep(1L, 0, "ADFIT", 1L, 1L);

        assertThat(slot.getFillNetwork()).isEqualTo("ADFIT");
        assertThat(slot.getPcAdUnitId()).isNull();
        assertThat(slot.getMobileAdUnitId()).isNull();
        assertThat(resolver.resolve(slot, true, List.of(adsense, adfit))).contains(adfit);
    }

    @Test
    void removingLastStepLeavesSlotEmpty() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));

        service.removeStep(1L, 0);

        assertThat(slot.getFillNetwork()).isNull();
        assertThat(slot.getPcAdUnitId()).isNull();
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
        assertThatThrownBy(() -> service.changeStep(1L, 0, "ADSENSE", null, 2L))
                .isInstanceOf(AdException.class);
        assertThat(slot.getPcAdUnitId()).isEqualTo(1L);
        assertThat(slot.getMobileAdUnitId()).isEqualTo(1L);
    }

    @Test
    void rejectUnknownNetworkDuplicateNetworkAndStaleIndex() {
        given(slots.findById(1L)).willReturn(Optional.of(slot()));
        stored.add(new AdSlotFillStep(1L, 2, "COUPANG", null, null));

        assertThatThrownBy(() -> service.changeStep(1L, 0, "INVALID", null, null))
                .isInstanceOf(AdException.class);
        assertThatThrownBy(() -> service.changeStep(1L, 1, "ADSENSE", null, null))
                .isInstanceOf(AdException.class);
        assertThatThrownBy(() -> service.moveStep(1L, 0, 5))
                .isInstanceOf(AdException.class);
        verify(steps, never()).deleteAllBySlotId(anyLong());
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
        service.changeStep(1L, 0, "ADSENSE", 1L, null);
        assertThat(slot.getPcAdUnitId()).isEqualTo(1L);
        assertThat(slot.getMobileAdUnitId()).isNull();
        service.changeStep(1L, 0, "ADSENSE", null, null);
        assertThat(slot.getPcAdUnitId()).isNull();
    }

    @Test
    void addStepAppendsFirstNetworkNotInChain() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));

        service.addStep(1L);

        assertThat(slot.getFillNetwork()).isEqualTo("ADSENSE");
        assertThat(stored).singleElement().satisfies(step -> {
            assertThat(step.getStepOrder()).isEqualTo(2);
            assertThat(step.getNetwork()).isEqualTo("COUPANG");
        });
    }

    @Test
    void laterStepKeepsItsOwnUnit() {
        given(slots.findById(1L)).willReturn(Optional.of(slot()));
        given(units.findById(3L)).willReturn(Optional.of(coupang));
        stored.add(new AdSlotFillStep(1L, 2, "COUPANG", null, null));

        service.changeStep(1L, 1, "COUPANG", 3L, null);

        assertThat(stored).singleElement().satisfies(step -> {
            assertThat(step.getPcAdUnitId()).isEqualTo(3L);
            assertThat(step.getMobileAdUnitId()).isNull();
        });
    }

    @Test
    void movingSecondStepToTopMakesItTheSlotNetwork() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        stored.add(new AdSlotFillStep(1L, 2, "COUPANG", 3L, null));

        service.moveStep(1L, 1, 0);

        assertThat(slot.getFillNetwork()).isEqualTo("COUPANG");
        assertThat(slot.getPcAdUnitId()).isEqualTo(3L);
        assertThat(stored).singleElement().satisfies(step -> {
            assertThat(step.getNetwork()).isEqualTo("ADSENSE");
            assertThat(step.getPcAdUnitId()).isEqualTo(1L);
        });
    }

    @Test
    void movingLastStepToMiddleShiftsTheRest() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        stored.add(new AdSlotFillStep(1L, 2, "MOBSENSE", null, null));
        stored.add(new AdSlotFillStep(1L, 3, "COUPANG", null, null));

        service.moveStep(1L, 2, 1);

        assertThat(slot.getFillNetwork()).isEqualTo("ADSENSE");
        assertThat(stored).extracting(AdSlotFillStep::getNetwork).containsExactly("COUPANG", "MOBSENSE");
        assertThat(stored).extracting(AdSlotFillStep::getStepOrder).containsExactly(2, 3);
    }

    @Test
    void removingFirstStepPromotesSecond() {
        AdSlot slot = slot();
        given(slots.findById(1L)).willReturn(Optional.of(slot));
        stored.add(new AdSlotFillStep(1L, 2, "MOBSENSE", null, null));
        stored.add(new AdSlotFillStep(1L, 3, "COUPANG", null, null));

        service.removeStep(1L, 0);

        assertThat(slot.getFillNetwork()).isEqualTo("MOBSENSE");
        assertThat(stored).extracting(AdSlotFillStep::getNetwork).containsExactly("COUPANG");
        assertThat(stored).extracting(AdSlotFillStep::getStepOrder).containsExactly(2);
    }

    private AdSlot slot() {
        return AdSlot.builder().id(1L).fillNetwork("ADSENSE")
                .pcWidth(300).pcHeight(600).mobileWidth(320).mobileHeight(100)
                .pcAdUnitId(1L).mobileAdUnitId(1L).build();
    }
}
