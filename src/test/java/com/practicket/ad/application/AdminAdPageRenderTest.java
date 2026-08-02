package com.practicket.ad.application;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.ad.application.AdminAdStatService.AdvertiserRow;
import com.practicket.ad.application.AdminAdStatService.BannerRow;
import com.practicket.ad.application.AdminAdStatService.BannerSummary;
import com.practicket.ad.application.AdminAdStatService.ChartBar;
import com.practicket.ad.application.AdminAdStatService.Dashboard;
import com.practicket.ad.application.AdminAdStatService.Period;
import com.practicket.ad.application.AdminAdStatService.SlotRow;
import com.practicket.ad.application.AdminAdStatService.SlotSummary;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerStatus;
import com.practicket.client.domain.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
                AdminBannerController.class,
                AdminSlotController.class,
                AdminAdvertiserController.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@ActiveProfiles("test")
// 애플리케이션에 @EnableJpaAuditing이 걸려 있어 MVC 슬라이스에서도 JPA 매핑 컨텍스트를 요구한다.
// 엔티티를 스캔하지 않는 슬라이스이므로 목으로 채운다.
@MockBean(JpaMetamodelMappingContext.class)
class AdminAdPageRenderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 26);
    private static final String ADVERTISER = "웰빙카페지압안마원";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminAdStatService adminAdStatService;

    @MockBean
    AdminBannerService adminBannerService;

    @MockBean
    AdSlotRepository adSlotRepository;

    @MockBean
    ClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        given(adminAdStatService.normalizePeriod(any(), any(), any(), any()))
                .willReturn(new Period(TODAY.minusDays(13), TODAY, 14));
        given(adminAdStatService.getDashboard(any(), any())).willReturn(dashboard());
        given(adminAdStatService.getBannerRows()).willReturn(List.of(bannerRow()));
        given(adminAdStatService.getSlotRows()).willReturn(List.of(new SlotRow(slot(), 1L)));
        given(adminAdStatService.getAdvertiserRows()).willReturn(List.of(advertiserRow()));
        given(adminAdStatService.getAdvertiserNames()).willReturn(List.of(ADVERTISER));
        given(adminAdStatService.getSlotOccupancies()).willReturn(List.of());
        given(adSlotRepository.findAll()).willReturn(List.of(slot()));
    }

    @Test
    @DisplayName("대시보드가 렌더된다.")
    void dashboardRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("대시보드")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("일별 노출 추이")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("성과 상위 캠페인")));
    }

    @Test
    @DisplayName("배너 목록이 렌더된다 — 성과 컬럼과 리포트 링크 버튼까지.")
    void bannerListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/banners"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/banner-list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ADVERTISER)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("링크 복사")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("노출중")));
    }

    @Test
    @DisplayName("배너 등록 폼이 렌더된다 — 광고주명 자동완성 목록 포함.")
    void bannerFormRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/banners/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/banner-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("advertiser-names")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("overlap-warning")));
    }

    @Test
    @DisplayName("슬롯 목록이 렌더된다.")
    void slotListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/slots"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/slot-list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PC_LEFT")));
    }

    @Test
    @DisplayName("광고주 목록이 렌더된다 — 통합 링크 버튼까지.")
    void advertiserListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/ad/advertisers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/advertiser-list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ADVERTISER)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("통합 링크")));
    }

    @Test
    @DisplayName("광고주 상세가 렌더된다 — 캠페인 이력과 통합 리포트 주소까지.")
    void advertiserDetailRenders() throws Exception {
        given(adminAdStatService.findAdvertiser(ADVERTISER))
                .willReturn(java.util.Optional.of(advertiserRow()));

        mockMvc.perform(get("/admin-hoya/ad/advertisers").param("name", ADVERTISER))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ad/advertiser-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("캠페인 이력")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/ad/report/advertiser/tok")));
    }

    @Test
    @DisplayName("없는 광고주를 보려 하면 목록으로 돌려보낸다.")
    void unknownAdvertiserRedirects() throws Exception {
        given(adminAdStatService.findAdvertiser("없는광고주")).willReturn(java.util.Optional.empty());

        mockMvc.perform(get("/admin-hoya/ad/advertisers").param("name", "없는광고주"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("빈 상태에서도 렌더된다 — 배너가 하나도 없는 첫 운영 시점.")
    void rendersWhenEmpty() throws Exception {
        given(adminAdStatService.getBannerRows()).willReturn(List.of());
        given(adminAdStatService.getAdvertiserRows()).willReturn(List.of());
        given(adminAdStatService.getSlotRows()).willReturn(List.of());
        given(adminAdStatService.getDashboard(any(), any())).willReturn(emptyDashboard());

        mockMvc.perform(get("/admin-hoya/ad")).andExpect(status().isOk());
        mockMvc.perform(get("/admin-hoya/ad/banners")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("등록된 배너가 없습니다")));
        mockMvc.perform(get("/admin-hoya/ad/slots")).andExpect(status().isOk());
        mockMvc.perform(get("/admin-hoya/ad/advertisers")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("구간이 길어 주 단위로 접힌 차트도 렌더된다.")
    void rendersWeeklyRolledUpChart() throws Exception {
        given(adminAdStatService.getDashboard(any(), any())).willReturn(weeklyDashboard());

        mockMvc.perform(get("/admin-hoya/ad").param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("주별 노출 추이")));
    }

    // ── 픽스처 ──

    private AdSlot slot() {
        return AdSlot.builder()
                .id(1L).code("PC_LEFT").name("PC 좌측").recommendedSize("300x600")
                .enabled(true).createdAt(TODAY.atStartOfDay())
                .build();
    }

    private Banner banner() {
        return Banner.builder()
                .id(11L).slot(slot()).imagePath("/ad-images/sample.png")
                .linkUrl("https://example.com").advertiserName(ADVERTISER)
                .startAt(TODAY.minusDays(3)).endAt(TODAY.plusDays(3)).enabled(true)
                .reportToken("report-token-1").createdAt(TODAY.atStartOfDay())
                .build();
    }

    private BannerRow bannerRow() {
        return new BannerRow(banner(), 1200L, 15L, 1.25, BannerStatus.LIVE);
    }

    private AdvertiserRow advertiserRow() {
        return new AdvertiserRow(ADVERTISER, 1, 1L, 1200L, 15L, 1.25,
                TODAY.minusDays(3), TODAY.plusDays(3), List.of(bannerRow()),
                "/ad/report/advertiser/tok123");
    }

    private Dashboard dashboard() {
        return new Dashboard(
                TODAY.minusDays(13), TODAY, 14,
                1200L, 15L, 1.25,
                12.4, -3.1, 0.05,
                TODAY.minusDays(27), TODAY.minusDays(14),
                List.of(new ChartBar(TODAY.minusDays(1), TODAY.minusDays(1), 500L, 6L, 60),
                        new ChartBar(TODAY, TODAY, 700L, 9L, 100)),
                false,
                List.of(bannerRow()),
                new BannerSummary(3, 1L),
                new SlotSummary(3, 1L));
    }

    /** 증감이 null인 경우(직전 기간 데이터 없음) — 템플릿의 null 분기를 태운다. */
    private Dashboard emptyDashboard() {
        return new Dashboard(
                TODAY.minusDays(13), TODAY, 14,
                0L, 0L, 0.0,
                null, null, null,
                TODAY.minusDays(27), TODAY.minusDays(14),
                List.of(new ChartBar(TODAY, TODAY, 0L, 0L, 0)),
                false,
                List.of(),
                new BannerSummary(0, 0L),
                new SlotSummary(3, 0L));
    }

    private Dashboard weeklyDashboard() {
        return new Dashboard(
                TODAY.minusDays(89), TODAY, 90,
                9000L, 120L, 1.33,
                5.0, 5.0, 0.0,
                TODAY.minusDays(179), TODAY.minusDays(90),
                List.of(new ChartBar(TODAY.minusDays(13), TODAY.minusDays(7), 4000L, 50L, 80),
                        new ChartBar(TODAY.minusDays(6), TODAY, 5000L, 70L, 100)),
                true,
                List.of(bannerRow()),
                new BannerSummary(3, 1L),
                new SlotSummary(3, 1L));
    }
}
