package com.practicket.ad.component;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.blog.application.BlogPostService;
import com.practicket.ad.domain.Banner;
import com.practicket.client.domain.ClientRepository;
import com.practicket.community.application.PopularTagService;
import com.practicket.community.application.PostCommentService;
import com.practicket.community.application.PostService;
import com.practicket.notice.application.NoticeService;
import com.practicket.ticket.application.TicketQueueService;
import com.practicket.view.ViewController;
import com.practicket.view.ViewControllerAdvice;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 광고 자리는 슬롯별로 따로 판단한다 — 한쪽이 비었다고 다른 쪽 배너를 끌어다 쓰지 않는다.
 * 권장 규격이 320x100 과 300x600 이라 자리를 바꿔 넣으면 contain 으로 뭉개진다
 * (600x1200 을 모바일 띠에 넣으면 폭 50px 로 줄어든다). 빈 자리는 쿠팡이 채운다.
 *
 * Thymeleaf 표현식 오류는 컴파일이 아니라 요청이 들어온 순간 터지므로 여기서 잡는다.
 * /terms 를 쓰는 이유는 모델 데이터 없이 layout/default(광고 fragment 포함)만 타는 가장 가벼운 페이지라서다.
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
class AdSectionRenderTest {

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

    @Test
    @DisplayName("모바일 슬롯만 비면 그 자리에 쿠팡이 들어가고 PC 배너는 넘어오지 않는다")
    void mobileEmpty() throws Exception {
        Banner pc = banner(1L);
        given(adSlotView.peek("MOBILE_TOP")).willReturn(null);
        given(adSlotView.peek("PC_LEFT")).willReturn(pc);

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ad-slot-link mobile-invisible")))
                .andExpect(content().string(containsString("coupang-ad-wrapper")))
                .andExpect(content().string(not(containsString("ad-slot-link desktop-invisible"))));
    }

    @Test
    @DisplayName("PC 슬롯만 비면 그 자리에 쿠팡이 들어가고 모바일 배너는 넘어오지 않는다")
    void pcEmpty() throws Exception {
        Banner mobile = banner(3L);
        given(adSlotView.peek("MOBILE_TOP")).willReturn(mobile);
        given(adSlotView.peek("PC_LEFT")).willReturn(null);

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ad-slot-link desktop-invisible")))
                .andExpect(content().string(containsString("coupang-ad-wrapper")))
                .andExpect(content().string(not(containsString("ad-slot-link mobile-invisible"))));
    }

    @Test
    @DisplayName("둘 다 비면 쿠팡 블록이 한 벌만 나온다")
    void bothEmpty() throws Exception {
        given(adSlotView.peek("MOBILE_TOP")).willReturn(null);
        given(adSlotView.peek("PC_LEFT")).willReturn(null);

        String html = mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 두 벌 나오면 쿠팡 스크립트가 두 번 초기화된다
        assertThat(html.split("coupang-ad-wrapper", -1).length - 1).isEqualTo(1);
    }

    @Test
    @DisplayName("둘 다 차 있으면 쿠팡은 나오지 않는다")
    void bothFilled() throws Exception {
        Banner mobile = banner(3L);
        Banner pc = banner(1L);
        given(adSlotView.peek("MOBILE_TOP")).willReturn(mobile);
        given(adSlotView.peek("PC_LEFT")).willReturn(pc);

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("coupang-ad-wrapper"))));
    }

    /** given(...) 인자 안에서 만들면 스터빙이 겹쳐 Mockito 가 거부한다 — 반드시 먼저 만들어 둘 것 */
    private Banner banner(Long id) {
        Banner banner = mock(Banner.class);
        given(banner.getId()).willReturn(id);
        given(banner.getAdvertiserName()).willReturn("테스트광고주");
        given(banner.getImagePath()).willReturn("/ad-images/test-" + id + ".png");
        return banner;
    }
}
