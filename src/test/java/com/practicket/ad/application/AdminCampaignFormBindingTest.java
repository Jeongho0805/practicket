package com.practicket.ad.application;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.ad.component.AdSlotSnapshotStore;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.client.domain.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 캠페인 폼은 계약 한 건과 배너 여러 줄을 한 번에 보낸다. 줄이 늘어난 만큼 이름이
 * {@code banners[0].slotId} 처럼 인덱스로 들어오는데, 이 바인딩은 컴파일러가 봐주지 않는다.
 * 파일 업로드가 섞인 multipart 라 더 그렇다.
 *
 * 배너 기간을 비운 채 보내는 것도 여기서 확인한다 — 빈 문자열이 날짜 변환에서 터지면
 * "계약 기간을 따른다"는 기본 동작 자체가 막힌다.
 */
@WebMvcTest(
        controllers = AdminCampaignController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
class AdminCampaignFormBindingTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminCampaignService adminCampaignService;
    @MockBean
    AdvertiserRepository advertiserRepository;
    @MockBean
    AdSlotRepository adSlotRepository;
    @MockBean
    ClientRepository clientRepository;
    @MockBean
    AdSlotSnapshotStore adSlotSnapshotStore;

    @Test
    @DisplayName("배너 여러 줄과 기기별 파일이 한 폼으로 들어온다")
    void bindsIndexedBannerRows() throws Exception {
        MockMultipartFile pcImage = new MockMultipartFile(
                "banners[0].pcImage", "pc.png", "image/png", "pc".getBytes());
        MockMultipartFile mobileImage = new MockMultipartFile(
                "banners[1].mobileImage", "mo.png", "image/png", "mo".getBytes());

        mockMvc.perform(multipart("/admin-hoya/ad/campaigns")
                        .file(pcImage)
                        .file(mobileImage)
                        .param("advertiserId", "1")
                        .param("name", "9월 프로모션")
                        .param("startAt", "2026-09-01")
                        .param("endAt", "2026-09-30")
                        .param("amount", "1500000")
                        .param("linkUrl", "https://example.com/event")
                        .param("banners[0].slotId", "1")
                        .param("banners[0].deleted", "false")
                        .param("banners[0].startAt", "2026-09-05")
                        .param("banners[0].endAt", "2026-09-20")
                        .param("_banners[0].enabled", "on")
                        .param("banners[0].enabled", "true")
                        .param("banners[1].slotId", "2")
                        .param("banners[1].deleted", "false")
                        // 기간을 비우면 계약 기간을 따른다
                        .param("banners[1].startAt", "")
                        .param("banners[1].endAt", "")
                        .param("_banners[1].enabled", "on"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<AdminCampaignService.CampaignForm> captor =
                ArgumentCaptor.forClass(AdminCampaignService.CampaignForm.class);
        then(adminCampaignService).should().save(captor.capture());

        AdminCampaignService.CampaignForm form = captor.getValue();
        assertThat(form.getName()).isEqualTo("9월 프로모션");
        assertThat(form.getStartAt()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(form.getAmount()).isEqualTo(1_500_000L);
        assertThat(form.getBanners()).hasSize(2);

        AdminCampaignService.BannerForm first = form.getBanners().get(0);
        assertThat(first.getSlotId()).isEqualTo(1L);
        assertThat(first.getStartAt()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(first.isEnabled()).isTrue();
        assertThat(first.getPcImage().isEmpty()).isFalse();

        AdminCampaignService.BannerForm second = form.getBanners().get(1);
        assertThat(second.getSlotId()).isEqualTo(2L);
        assertThat(second.getStartAt()).isNull();
        assertThat(second.getEndAt()).isNull();
        assertThat(second.getMobileImage().isEmpty()).isFalse();
    }

    @Test
    @DisplayName("노출 체크를 풀면 꺼진 상태로 들어온다 — 체크박스는 안 보내면 값이 없다")
    void uncheckedEnabledBecomesFalse() throws Exception {
        mockMvc.perform(multipart("/admin-hoya/ad/campaigns")
                        .param("advertiserId", "1")
                        .param("name", "중지 캠페인")
                        .param("startAt", "2026-09-01")
                        .param("endAt", "2026-09-30")
                        .param("banners[0].slotId", "1")
                        .param("banners[0].deleted", "false")
                        .param("_banners[0].enabled", "on"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<AdminCampaignService.CampaignForm> captor =
                ArgumentCaptor.forClass(AdminCampaignService.CampaignForm.class);
        then(adminCampaignService).should().save(captor.capture());

        assertThat(captor.getValue().getBanners().get(0).isEnabled()).isFalse();
    }
}
