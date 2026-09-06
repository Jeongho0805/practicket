package com.practicket.ad.application;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.ad.application.AdminAdStatService.ChartBar;
import com.practicket.ad.application.AdminAdStatService.Dashboard;
import com.practicket.ad.application.AdminAdStatService.Period;
import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.BannerStatus;
import com.practicket.ad.domain.CampaignStatus;
import com.practicket.client.domain.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 어드민 화면이 실제로 렌더되는지 본다.
 *
 * Thymeleaf 오류는 컴파일이 아니라 <b>요청이 들어온 순간</b> 터진다(표현식 오타, 금지된 인라인 등).
 * 어드민은 로그인 가드 뒤에 있어 사람이 눌러보기 전까지 아무도 모르는데,
 * 실제로 그렇게 500이 난 적이 있어 여기서 잡는다.
 *
 * {@link AdminNavAdvice}는 제외한다 — 사이드바 배지 숫자는 다른 기능(공지 등)의 리포지토리까지
 * 끌어와서, 이 테스트가 광고와 무관한 변경에 깨지게 만든다. 템플릿은 배지가 없어도 렌더된다.
 */
@WebMvcTest(
        controllers = {
                AdminDashboardController.class,
                AdminCampaignController.class,
                AdminAdvertiserController.class,
                AdminSlotController.class,
                AdminUnitController.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@ActiveProfiles("test")
// 애플리케이션에 @EnableJpaAuditing이 걸려 있어 MVC 슬라이스에서도 JPA 매핑 컨텍스트를 요구한다.
@MockBean(JpaMetamodelMappingContext.class)
class AdminAdPageRenderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 6);
    private static final String ADVERTISER = "웰빙카페지압안마원";
    private static final String CAMPAIGN = "9월 티켓오픈 프로모션";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminAdStatService adminAdStatService;
    @MockBean
    AdminCampaignService adminCampaignService;
    @MockBean
    AdminAdvertiserService adminAdvertiserService;
    @MockBean
    AdminSlotService adminSlotService;
    @MockBean
    AdvertiserRepository advertiserRepository;
    @MockBean
    AdSlotRepository adSlotRepository;
    @MockBean
    AdUnitRepository adUnitRepository;
    /** WebConfig 가 요구한다 */
    @MockBean
    ClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        given(adminAdStatService.normalizePeriod(any(), any(), any(), any()))
                .willReturn(new Period(TODAY.minusDays(13), TODAY, 14));
        given(adminAdStatService.getDashboard(any(), any())).willReturn(dashboard());
        given(adminCampaignService.getCampaignRows()).willReturn(List.of(campaignRow()));
        given(adminCampaignService.findDetail(1L)).willReturn(Optional.of(campaignDetail()));
        given(adminCampaignService.blankForm()).willReturn(blankForm());
        given(adminCampaignService.findForm(1L)).willReturn(Optional.of(blankForm()));
        given(adminAdvertiserService.getRows()).willReturn(List.of(advertiserRow()));
        given(adminAdvertiserService.findDetail(1L)).willReturn(Optional.of(advertiserDetail()));
        given(adminSlotService.getGroups()).willReturn(List.of(slotGroup()));
        given(adminSlotService.findForm(1L)).willReturn(Optional.of(new AdminSlotService.SlotForm()));
        given(advertiserRepository.findAllByOrderByNameAsc()).willReturn(List.of(advertiser()));
        given(adSlotRepository.findAll()).willReturn(List.of(slot()));
        given(adUnitRepository.findAllByOrderByNetworkAscNameAsc()).willReturn(List.of(unit()));
    }

    @Test
    @DisplayName("대시보드가 렌더된다 — 계약 금액과 캠페인 표까지")
    void dashboardRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/dashboard"))
                .andExpect(content().string(containsString("진행중 계약 금액")))
                .andExpect(content().string(containsString("일별 노출 추이")))
                .andExpect(content().string(containsString(CAMPAIGN)));
    }

    @Test
    @DisplayName("광고단위가 빈 슬롯이 있으면 대시보드가 경고한다")
    void dashboardWarnsUnitGap() throws Exception {
        given(adminSlotService.getGroups()).willReturn(List.of(
                new AdminSlotService.SlotGroup("전 페이지 공통", "GLOBAL", List.of(
                        new AdminSlotService.SlotRow(slot(), 0L, "300x600", null, null, null, true, false)))));

        mockMvc.perform(get("/admin-hoya/ad"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("광고단위가 비어 있는 슬롯이 있습니다")));
    }

    @Test
    @DisplayName("캠페인 목록이 렌더된다")
    void campaignListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/campaigns"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/campaign-list"))
                .andExpect(content().string(containsString(CAMPAIGN)))
                .andExpect(content().string(containsString(ADVERTISER)));
    }

    @Test
    @DisplayName("캠페인 상세가 렌더된다 — 기간 타임라인과 리포트 링크까지")
    void campaignDetailRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/campaign-detail"))
                .andExpect(content().string(containsString("기간 비교")))
                .andExpect(content().string(containsString("/ad/report/campaign/TOKEN")));
    }

    @Test
    @DisplayName("캠페인 등록 폼이 렌더된다 — 슬롯 줄 템플릿까지")
    void campaignFormRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/campaigns/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/campaign-form"))
                .andExpect(content().string(containsString("banner-row-template")))
                .andExpect(content().string(containsString("슬롯별 배너")));
    }

    @Test
    @DisplayName("없는 캠페인을 보려 하면 목록으로 돌려보낸다")
    void unknownCampaignRedirects() throws Exception {
        given(adminCampaignService.findDetail(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/admin-hoya/ad/campaigns/99"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("광고주 목록·상세·폼이 렌더된다")
    void advertiserScreensRender() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/advertisers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/advertiser-list"))
                .andExpect(content().string(containsString(ADVERTISER)));

        mockMvc.perform(get("/admin-hoya/ad/advertisers/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/advertiser-detail"))
                .andExpect(content().string(containsString("캠페인 이력")));

        mockMvc.perform(get("/admin-hoya/ad/advertisers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/advertiser-form"));
    }

    @Test
    @DisplayName("슬롯 목록·수정 폼이 렌더된다")
    void slotScreensRender() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/slots"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/slot-list"))
                .andExpect(content().string(containsString("PC_LEFT")));

        mockMvc.perform(get("/admin-hoya/ad/slots/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/slot-form"));
    }

    @Test
    @DisplayName("광고 설정 목록·폼이 렌더된다")
    void unitScreensRender() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/units"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/unit-list"))
                .andExpect(content().string(containsString("9697904962")));

        mockMvc.perform(get("/admin-hoya/ad/units/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/unit-form"));
    }

    @Test
    @DisplayName("빈 상태에서도 렌더된다 — 아무것도 팔지 않은 첫 운영 시점")
    void rendersWhenEmpty() throws Exception {
        given(adminCampaignService.getCampaignRows()).willReturn(List.of());
        given(adminAdvertiserService.getRows()).willReturn(List.of());
        given(adminSlotService.getGroups()).willReturn(List.of());
        given(adminAdvertiserService.findDetail(1L)).willReturn(Optional.of(
                new AdminAdvertiserService.AdvertiserDetail(advertiser(), List.of(), 0L, 0L, 0L)));

        mockMvc.perform(get("/admin-hoya/ad")).andExpect(status().isOk());
        mockMvc.perform(get("/admin-hoya/ad/campaigns")).andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 캠페인이 없습니다")));
        mockMvc.perform(get("/admin-hoya/ad/advertisers")).andExpect(status().isOk())
                .andExpect(content().string(containsString("등록된 광고주가 없습니다")));
        mockMvc.perform(get("/admin-hoya/ad/advertisers/1")).andExpect(status().isOk());
        mockMvc.perform(get("/admin-hoya/ad/slots")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("구간이 길어 주 단위로 접힌 차트도 렌더된다")
    void rendersWeeklyRolledUpChart() throws Exception {
        given(adminAdStatService.getDashboard(any(), any())).willReturn(weeklyDashboard());

        mockMvc.perform(get("/admin-hoya/ad").param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("주별 노출 추이")));
    }

    // ── 픽스처 ──

    private AdSlot slot() {
        return AdSlot.builder()
                .id(1L).code("PC_LEFT").name("데스크톱 좌측")
                .pcWidth(300).pcHeight(600).format("디스플레이")
                .groupName("전 페이지 공통").groupPath("GLOBAL")
                .sortOrder(1).fillNetwork("COUPANG")
                .enabled(true).createdAt(TODAY.atStartOfDay())
                .build();
    }

    private AdUnit unit() {
        return AdUnit.builder()
                .id(1L).network("ADSENSE").unitId("9697904962").name("애드센스 기본")
                .isDefault(true).createdAt(TODAY.atStartOfDay())
                .build();
    }

    private Advertiser advertiser() {
        return Advertiser.builder()
                .id(1L).name(ADVERTISER).manager("김담당").createdAt(TODAY.atStartOfDay())
                .build();
    }

    private AdCampaign campaign() {
        return AdCampaign.builder()
                .id(1L).advertiserId(1L).name(CAMPAIGN)
                .startAt(TODAY.minusDays(5)).endAt(TODAY.plusDays(25))
                .amount(1_500_000L).reportToken("TOKEN").memo("메모")
                .createdAt(TODAY.atStartOfDay())
                .build();
    }

    private AdminCampaignService.CampaignRow campaignRow() {
        return new AdminCampaignService.CampaignRow(1L, CAMPAIGN, ADVERTISER,
                TODAY.minusDays(5), TODAY.plusDays(25), 1_500_000L, CampaignStatus.LIVE,
                2, "데스크톱 좌측 · 모바일 상단", 1L, 1200L, 15L, 1.25, 25L);
    }

    private AdminCampaignService.BannerRow bannerRow() {
        return new AdminCampaignService.BannerRow(11L, "PC_LEFT", "데스크톱 좌측",
                "/ad-images/pc.png", null, "300x600", null,
                TODAY.minusDays(5), TODAY.plusDays(25), false, 31L, true, BannerStatus.LIVE,
                1200L, 15L, 1.25, 0.0, 100.0, List.of("겹치는 캠페인"));
    }

    private AdminCampaignService.CampaignDetail campaignDetail() {
        return new AdminCampaignService.CampaignDetail(campaign(), advertiser(), CampaignStatus.LIVE,
                List.of(bannerRow()), 1200L, 15L, 1.25, 25L, 16.0);
    }

    private AdminCampaignService.CampaignForm blankForm() {
        AdminCampaignService.CampaignForm form = new AdminCampaignService.CampaignForm();
        form.setStartAt(TODAY);
        form.setEndAt(TODAY.plusDays(30));
        form.setAmount(0L);
        form.setAdvertiserId(1L);
        form.getBanners().add(new AdminCampaignService.BannerForm());
        return form;
    }

    private AdminAdvertiserService.AdvertiserRow advertiserRow() {
        return new AdminAdvertiserService.AdvertiserRow(advertiser(), 2, 1L, 2_400_000L,
                1200L, 15L, 1.25, TODAY.minusDays(30), TODAY.plusDays(25));
    }

    private AdminAdvertiserService.AdvertiserDetail advertiserDetail() {
        return new AdminAdvertiserService.AdvertiserDetail(advertiser(),
                List.of(new AdminAdvertiserService.CampaignHistory(campaign(), CampaignStatus.LIVE, 2, 1200L, 15L, 1.25)),
                1_500_000L, 1200L, 15L);
    }

    private AdminSlotService.SlotGroup slotGroup() {
        return new AdminSlotService.SlotGroup("전 페이지 공통", "GLOBAL", List.of(
                new AdminSlotService.SlotRow(slot(), 1L, "300x600", null, unit(), null, false, false)));
    }

    private Dashboard dashboard() {
        return new Dashboard(
                TODAY.minusDays(13), TODAY, 14,
                1200L, 15L, 1.25,
                12.4, -3.1, 0.05,
                TODAY.minusDays(27), TODAY.minusDays(14),
                List.of(new ChartBar(TODAY.minusDays(1), TODAY.minusDays(1), 500L, 6L, 60),
                        new ChartBar(TODAY, TODAY, 700L, 9L, 100)),
                false);
    }

    private Dashboard weeklyDashboard() {
        return new Dashboard(
                TODAY.minusDays(89), TODAY, 90,
                9000L, 120L, 1.33,
                5.0, 5.0, 0.0,
                TODAY.minusDays(179), TODAY.minusDays(90),
                List.of(new ChartBar(TODAY.minusDays(13), TODAY.minusDays(7), 4000L, 50L, 80),
                        new ChartBar(TODAY.minusDays(6), TODAY, 5000L, 70L, 100)),
                true);
    }
}
