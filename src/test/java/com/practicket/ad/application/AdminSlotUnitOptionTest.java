package com.practicket.ad.application;

import com.practicket.ad.application.AdminSlotService.SlotForm;
import com.practicket.ad.application.AdminSlotService.UnitOption;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 슬롯 폼이 광고단위마다 붙이는 표시. 규격 비교는 막지 않고 알리기만 하므로
 * 문구가 틀리면 아무도 모른 채 잘린 광고가 나간다.
 */
@ExtendWith(MockitoExtension.class)
class AdminSlotUnitOptionTest {

    @Mock
    AdUnitRepository adUnitRepository;

    @InjectMocks
    AdminSlotService service;

    @Test
    @DisplayName("슬롯보다 큰 단위에만 이유가 붙는다")
    void marksOnlyOversize() {
        given(adUnitRepository.findAllByOrderByNetworkAscNameAsc())
                .willReturn(List.of(responsive(), fitting(), oversize()));

        List<UnitOption> options = service.unitOptions(mobileSlot(320, 100));

        assertThat(options).extracting(UnitOption::mobileLabel)
                .anySatisfy(label -> assertThat(label).doesNotContain("슬롯보다 큼"));
        assertThat(options.get(2).mobileLabel()).startsWith("슬롯보다 큼 — ");
        assertThat(options.get(0).mobileLabel()).doesNotContain("—");
        assertThat(options.get(1).mobileLabel()).doesNotContain("—");
    }

    @Test
    @DisplayName("규격이 빈 기기는 어떤 단위에도 이유를 붙이지 않는다")
    void skipsDeviceWithoutSize() {
        given(adUnitRepository.findAllByOrderByNetworkAscNameAsc())
                .willReturn(List.of(responsive(), oversize()));

        List<UnitOption> options = service.unitOptions(mobileSlot(320, 100));

        assertThat(options).noneMatch(UnitOption::isPcTooBig);
    }

    @Test
    @DisplayName("고른 단위가 슬롯보다 크면 경고 문구를 만든다")
    void warnsWhenChosenUnitIsOversize() {
        SlotForm form = mobileSlot(320, 100);
        form.setMobileAdUnitId(3L);
        given(adUnitRepository.findAllById(List.of(3L))).willReturn(List.of(oversize()));

        assertThat(service.oversizeWarning(form)).startsWith("모바일 광고단위가 슬롯보다 큽니다.");
    }

    @Test
    @DisplayName("고른 단위가 들어가면 경고가 없다")
    void noWarningWhenChosenUnitFits() {
        SlotForm form = mobileSlot(320, 100);
        form.setMobileAdUnitId(2L);
        given(adUnitRepository.findAllById(List.of(2L))).willReturn(List.of(fitting()));

        assertThat(service.oversizeWarning(form)).isNull();
    }

    private SlotForm mobileSlot(Integer width, Integer height) {
        SlotForm form = new SlotForm();
        form.setMobileWidth(width);
        form.setMobileHeight(height);
        return form;
    }

    private AdUnit responsive() {
        return AdUnit.builder().id(1L).network("ADSENSE").unitId("9697904962").name("애드센스 기본")
                .createdAt(LocalDateTime.now()).build();
    }

    private AdUnit fitting() {
        return AdUnit.builder().id(2L).network("ADFIT").unitId("DAN-ZwiDsGtn17OqUL3X").name("모바일 띠")
                .width(320).height(100).createdAt(LocalDateTime.now()).build();
    }

    private AdUnit oversize() {
        return AdUnit.builder().id(3L).network("ADFIT").unitId("DAN-BicR0BE99mzbLjlm").name("PC 세로형")
                .width(160).height(600).createdAt(LocalDateTime.now()).build();
    }
}
