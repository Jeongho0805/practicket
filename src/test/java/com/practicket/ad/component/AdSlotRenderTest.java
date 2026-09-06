package com.practicket.ad.component;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.blog.application.BlogPostService;
import com.practicket.client.domain.ClientRepository;
import com.practicket.community.application.PopularTagService;
import com.practicket.community.application.PostCommentService;
import com.practicket.community.application.PostService;
import com.practicket.notice.application.NoticeService;
import com.practicket.ticket.application.TicketQueueService;
import com.practicket.view.ViewController;
import com.practicket.view.ViewControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 광고 자리는 자리별로 따로 판단한다 — 한쪽이 비었다고 다른 쪽 배너를 끌어다 쓰지 않는다.
 * 그리고 네트워크 코드는 서버가 절대 내보내지 않는다. 안 보이는 자리에 광고 코드를 넣는 것은
 * 애드센스 정책 위반이라, 실제로 보이는지 아는 브라우저가 /js/ad-fill.js 로 넣는다.
 *
 * Thymeleaf 표현식 오류는 컴파일이 아니라 요청이 들어온 순간 터지므로 여기서 잡는다.
 * /terms 를 쓰는 이유는 모델 데이터 없이 layout/default(광고 조각 포함)만 타는 가장 가벼운 페이지라서다.
 */
@WebMvcTest(
        controllers = ViewController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@Import(ViewControllerAdvice.class)
@ActiveProfiles("test")
// 애플리케이션에 @EnableJpaAuditing이 걸려 있어 MVC 슬라이스에서도 JPA 매핑 컨텍스트를 요구한다.
@MockBean(JpaMetamodelMappingContext.class)
class AdSlotRenderTest {

    private static final String BLANK_PIXEL = "data:image/gif;base64,";

    @Autowired
    private MockMvc mockMvc;

    /** 템플릿이 @adSlotView 로 이름을 찍어 찾으므로 빈 이름을 명시한다 */
    @MockBean(name = "adSlotView")
    private AdSlotView adSlotView;

    @MockBean
    private TicketQueueService ticketQueueService;
    @MockBean
    private BlogPostService blogPostService;
    @MockBean
    private PostService postService;
    @MockBean
    private PostCommentService postCommentService;
    @MockBean
    private PopularTagService popularTagService;
    @MockBean
    private NoticeService noticeService;
    /** WebConfig 가 요구한다 */
    @MockBean
    private ClientRepository clientRepository;

    @BeforeEach
    void emptyByDefault() {
        given(adSlotView.render("MOBILE_TOP")).willReturn(AdSlotRender.empty("MOBILE_TOP"));
        given(adSlotView.render("PC_LEFT")).willReturn(AdSlotRender.empty("PC_LEFT"));
        given(adSlotView.render("PC_RIGHT")).willReturn(AdSlotRender.empty("PC_RIGHT"));
    }

    @Test
    @DisplayName("네트워크가 채우는 자리에도 광고 코드는 안 나가고 자리 정보만 나간다")
    void fillEmitsNoNetworkCode() throws Exception {
        given(adSlotView.render("PC_RIGHT")).willReturn(new AdSlotRender("PC_RIGHT",
                AdFace.fill(true, unit("ADSENSE", "9697904962"), "ca-pub-1", null),
                AdFace.none()));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-network=\"ADSENSE\"")))
                .andExpect(content().string(not(containsString("adsbygoogle"))))
                .andExpect(content().string(not(containsString("pagead2.googlesyndication.com"))));
    }

    @Test
    @DisplayName("쿠팡도 마찬가지다 — 스크립트가 아니라 자리만 나간다")
    void coupangEmitsNoNetworkCode() throws Exception {
        given(adSlotView.render("PC_LEFT")).willReturn(new AdSlotRender("PC_LEFT",
                AdFace.fill(true, unit("COUPANG", "943782"), "AF2647827", "carousel"),
                AdFace.none()));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-network=\"COUPANG\"")))
                .andExpect(content().string(not(containsString("ads-partners.coupang.com"))))
                .andExpect(content().string(not(containsString("PartnersCoupang"))));
    }

    @Test
    @DisplayName("프로파일이 막으면 자리 정보조차 안 나간다")
    void fillDisabledEmitsNothing() throws Exception {
        given(adSlotView.render("PC_LEFT")).willReturn(new AdSlotRender("PC_LEFT",
                AdFace.fill(false, unit("COUPANG", "943782"), "AF2647827", "carousel"),
                AdFace.none()));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-network"))))
                // 자리 자체는 남아야 데스크톱 3열 배치가 안 무너진다
                .andExpect(content().string(containsString("pc-fill pc-net-COUPANG")));
    }

    @Test
    @DisplayName("모바일 자리에 판 배너는 데스크톱 자리로 넘어오지 않는다")
    void slotsDoNotBorrowBanners() throws Exception {
        given(adSlotView.render("MOBILE_TOP")).willReturn(new AdSlotRender("MOBILE_TOP",
                AdFace.none(),
                AdFace.banner(3L, "테스트광고주", "/ad-images/m.png")));

        String html = mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/ad-images/m.png");
        assertThat(html.split("data-banner-id", -1).length - 1).isEqualTo(1);
    }

    @Test
    @DisplayName("한쪽 기기만 그림이 있으면 반대 기기는 투명 픽셀로 떨어져 그림을 안 받아간다")
    void singleDeviceImageUsesBlankPixel() throws Exception {
        given(adSlotView.render("PC_LEFT")).willReturn(new AdSlotRender("PC_LEFT",
                AdFace.banner(1L, "테스트광고주", "/ad-images/pc.png"),
                AdFace.none()));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("media=\"(min-width: 769px)\"")))
                .andExpect(content().string(containsString("srcset=\"/ad-images/pc.png\"")))
                .andExpect(content().string(containsString(BLANK_PIXEL)));
    }

    @Test
    @DisplayName("두 기기가 같은 배너면 picture 하나로 묶어 한 장만 받게 한다")
    void bothDevicesShareOnePicture() throws Exception {
        given(adSlotView.render("PC_LEFT")).willReturn(new AdSlotRender("PC_LEFT",
                AdFace.banner(7L, "테스트광고주", "/ad-images/pc.png"),
                AdFace.banner(7L, "테스트광고주", "/ad-images/m.png")));

        String html = mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("srcset=\"/ad-images/m.png\"");
        assertThat(html).contains("src=\"/ad-images/pc.png\"");
        assertThat(html).doesNotContain(BLANK_PIXEL);
        assertThat(html.split("data-banner-id", -1).length - 1).isEqualTo(1);
    }

    private AdSlotSnapshot.Unit unit(String network, String unitId) {
        return new AdSlotSnapshot.Unit(network, unitId, null, null);
    }
}
