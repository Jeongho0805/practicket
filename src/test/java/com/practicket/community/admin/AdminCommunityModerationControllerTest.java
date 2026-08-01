package com.practicket.community.admin;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.community.admin.dto.BanDuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 신고함·글댓글 관리가 공유하는 처리 엔드포인트. redirectTo 검증과 액션 위임만 확인한다 —
 * 실제 도메인 로직은 {@link AdminCommunityServiceTest} 가 맡는다.
 */
@WebMvcTest(
        controllers = AdminCommunityModerationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
class AdminCommunityModerationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminCommunityService adminCommunityService;

    @Test
    @DisplayName("블라인드 처리 후 redirectTo가 어드민 커뮤니티 경로면 그대로 되돌아간다.")
    void blindPost_redirectsToGivenPath() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/posts/1/blind")
                        .param("redirectTo", "/admin-hoya/community/posts?type=POST&page=2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin-hoya/community/posts?type=POST&page=2"));

        verify(adminCommunityService).blindPost(1L);
    }

    @Test
    @DisplayName("redirectTo가 어드민 커뮤니티 경로가 아니면 신고함 기본 주소로 되돌린다 — 오픈 리다이렉트 방지.")
    void blindPost_rejectsForeignRedirect() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/posts/1/blind")
                        .param("redirectTo", "https://evil.example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin-hoya/community/reports"));
    }

    @Test
    @DisplayName("redirectTo가 없으면 신고함 기본 주소로 되돌린다.")
    void blindPost_noRedirectTo_fallsBackToDefault() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/posts/1/blind"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin-hoya/community/reports"));
    }

    @Test
    @DisplayName("존재하지 않는 글을 삭제하려 하면 500이 아니라 플래시 에러와 함께 되돌아간다.")
    void deletePost_notFound_redirectsWithFlashError() throws Exception {
        willThrow(new IllegalArgumentException("존재하지 않거나 이미 삭제된 글입니다. id=999"))
                .given(adminCommunityService).deletePost(999L);

        mockMvc.perform(post("/admin-hoya/community/posts/999/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin-hoya/community/reports"));
    }

    @Test
    @DisplayName("밴 요청은 기간·사유를 그대로 서비스에 위임한다.")
    void banClient_delegatesDurationAndReason() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/clients/10/ban")
                        .param("duration", "ONE_DAY")
                        .param("reason", "도배"))
                .andExpect(status().is3xxRedirection());

        verify(adminCommunityService).banClient(10L, BanDuration.ONE_DAY, "도배");
    }

    @Test
    @DisplayName("밴 사유가 빈 값이면 null로 정규화해 넘긴다.")
    void banClient_blankReasonNormalizedToNull() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/clients/10/ban")
                        .param("duration", "PERMANENT")
                        .param("reason", "   "))
                .andExpect(status().is3xxRedirection());

        verify(adminCommunityService).banClient(eq(10L), eq(BanDuration.PERMANENT), isNull());
    }

    @Test
    @DisplayName("밴 해제는 unbanClient로 위임한다.")
    void unbanClient_delegates() throws Exception {
        mockMvc.perform(post("/admin-hoya/community/clients/10/unban"))
                .andExpect(status().is3xxRedirection());

        verify(adminCommunityService).unbanClient(10L);
    }
}
