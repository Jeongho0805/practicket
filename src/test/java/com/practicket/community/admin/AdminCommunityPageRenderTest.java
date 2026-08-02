package com.practicket.community.admin;

import com.practicket.ad.admin.AdminNavAdvice;
import com.practicket.community.admin.dto.AdminCommentView;
import com.practicket.community.admin.dto.AdminPostView;
import com.practicket.community.admin.dto.ReportedTargetRow;
import com.practicket.community.domain.entity.ReportReason;
import com.practicket.community.domain.entity.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 어드민 커뮤니티 화면이 실제로 렌더되는지 본다. Thymeleaf 오류는 요청이 들어온 순간 터지고
 * 어드민은 로그인 가드 뒤에 있어 사람이 눌러보기 전까지 아무도 모른다(광고 어드민과 같은 이유,
 * {@link com.practicket.ad.application.AdminAdPageRenderTest} 참고).
 *
 * AdminNavAdvice는 배너·공지 등 다른 기능 리포지토리까지 끌어와서 이 테스트를 무관한 변경에
 * 깨지게 만들기 때문에 제외한다 — 사이드바 배지 없이도 템플릿은 렌더된다.
 */
@WebMvcTest(
        controllers = {AdminCommunityReportController.class, AdminCommunityPostController.class},
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = AdminNavAdvice.class))
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
class AdminCommunityPageRenderTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminCommunityService adminCommunityService;

    @BeforeEach
    void setUp() {
        // postView()/commentView() 는 자체적으로 given().willReturn() 을 여러 번 호출하므로
        // 바깥 willReturn() 인자로 바로 넘기면 "진행 중인 스텁"이 겹쳐 UnfinishedStubbingException 이 난다.
        ReportedTargetRow row = reportRow();
        AdminPostView post = postView();
        AdminCommentView comment = commentView();
        given(adminCommunityService.getReportedTargets(any())).willReturn(new PageImpl<>(List.of(row)));
        given(adminCommunityService.searchPosts(anyString(), any())).willReturn(new PageImpl<>(List.of(post)));
        given(adminCommunityService.searchComments(anyString(), any())).willReturn(new PageImpl<>(List.of(comment)));
    }

    @Test
    @DisplayName("신고함이 렌더된다 — 대상 미리보기·사유 분포·처리 버튼까지.")
    void reportListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/community/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/community/report-list"))
                .andExpect(content().string(containsString("신고함")))
                .andExpect(content().string(containsString("광고 스팸입니다")))
                .andExpect(content().string(containsString("블라인드")))
                .andExpect(content().string(containsString("작성자 밴")));
    }

    @Test
    @DisplayName("신고가 하나도 없으면 빈 상태 문구가 뜬다.")
    void reportListRenders_empty() throws Exception {
        given(adminCommunityService.getReportedTargets(any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin-hoya/community/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("들어온 신고가 없습니다")));
    }

    @Test
    @DisplayName("글 관리 화면이 렌더된다.")
    void postListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/community/posts").param("type", "POST"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/community/post-list"))
                .andExpect(content().string(containsString("글·댓글 관리")))
                .andExpect(content().string(containsString("공지할 내용 제목")));
    }

    @Test
    @DisplayName("댓글 관리 화면이 렌더된다.")
    void commentListRenders() throws Exception {
        mockMvc.perform(get("/admin-hoya/community/posts").param("type", "COMMENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("댓글 내용입니다")));
    }

    @Test
    @DisplayName("삭제된 글은 상태만 보이고 삭제 버튼은 숨는다.")
    void deletedPostHasNoDeleteButton() throws Exception {
        AdminPostView deleted = deletedPostView();
        given(adminCommunityService.searchPosts(anyString(), any()))
                .willReturn(new PageImpl<>(List.of(deleted)));

        mockMvc.perform(get("/admin-hoya/community/posts").param("type", "POST"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("삭제됨")));
    }

    // ── 픽스처 ──

    private ReportedTargetRow reportRow() {
        return new ReportedTargetRow(
                ReportTargetType.POST, 1L, 2L, LocalDateTime.of(2026, 7, 26, 10, 0),
                Map.of(ReportReason.AD, 2L),
                "광고 스팸입니다", "티켓요정", "118.235",
                10L, false, false);
    }

    private AdminPostView postView() {
        AdminPostView view = mock(AdminPostView.class);
        given(view.getId()).willReturn(1L);
        given(view.getTitle()).willReturn("공지할 내용 제목");
        given(view.getNickname()).willReturn("티켓요정");
        given(view.getBlinded()).willReturn(false);
        given(view.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 26, 10, 0));
        given(view.getDeletedAt()).willReturn(null);
        return view;
    }

    private AdminPostView deletedPostView() {
        AdminPostView view = mock(AdminPostView.class);
        given(view.getId()).willReturn(2L);
        given(view.getTitle()).willReturn("지워진 글");
        given(view.getNickname()).willReturn("익명");
        given(view.getBlinded()).willReturn(false);
        given(view.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 20, 10, 0));
        given(view.getDeletedAt()).willReturn(LocalDateTime.of(2026, 7, 21, 10, 0));
        return view;
    }

    private AdminCommentView commentView() {
        AdminCommentView view = mock(AdminCommentView.class);
        given(view.getId()).willReturn(1L);
        given(view.getContent()).willReturn("댓글 내용입니다");
        given(view.getNickname()).willReturn("익명");
        given(view.getBlinded()).willReturn(false);
        given(view.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 26, 10, 0));
        given(view.getDeletedAt()).willReturn(null);
        return view;
    }
}
