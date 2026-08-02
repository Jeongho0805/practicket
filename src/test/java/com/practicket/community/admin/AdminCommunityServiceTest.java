package com.practicket.community.admin;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.community.admin.dto.AdminCommentView;
import com.practicket.community.admin.dto.AdminPostView;
import com.practicket.community.admin.dto.BanDuration;
import com.practicket.community.admin.dto.ReportedTargetRow;
import com.practicket.community.admin.repository.AdminCommunityPurgeRepository;
import com.practicket.community.admin.repository.AdminPostCommentQueryRepository;
import com.practicket.community.admin.repository.AdminPostQueryRepository;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import com.practicket.community.domain.entity.PostReport;
import com.practicket.community.domain.entity.ReportReason;
import com.practicket.community.domain.entity.ReportTargetType;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostReportRepository;
import com.practicket.community.domain.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 어드민 커뮤니티 모더레이션 서비스 단위 테스트.
 * DB 대신 리포지토리를 목으로 채워 신고함 조립·블라인드·강제삭제·밴 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminCommunityServiceTest {

    private static final Long POST_ID = 1L;
    private static final Long COMMENT_ID = 2L;
    private static final Long CLIENT_ID = 10L;

    @InjectMocks
    private AdminCommunityService adminCommunityService;

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository postCommentRepository;
    @Mock
    private PostReportRepository postReportRepository;
    @Mock
    private AdminPostQueryRepository adminPostQueryRepository;
    @Mock
    private AdminPostCommentQueryRepository adminPostCommentQueryRepository;
    @Mock
    private AdminCommunityPurgeRepository adminCommunityPurgeRepository;
    @Mock
    private ClientRepository clientRepository;

    // ============ 신고함 조립 ============

    @Test
    @DisplayName("신고 대상이 글이면 제목·닉네임·사유 분포를 담아 한 줄로 만든다.")
    void reportedTargets_post() {
        PostReportRepository.ReportedTarget target = reportedTarget(ReportTargetType.POST, POST_ID, 2L,
                LocalDateTime.of(2026, 7, 26, 10, 0));
        given(postReportRepository.findReportedTargets(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(target)));
        given(postReportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.POST, POST_ID))
                .willReturn(List.of(report(ReportReason.AD), report(ReportReason.AD), report(ReportReason.ABUSE)));
        // postView()가 자체적으로 given().willReturn()을 여러 번 호출하므로, 바깥 willReturn()의
        // 인자로 바로 넘기면 "진행 중인 스텁"이 겹쳐 UnfinishedStubbingException이 난다 — 미리 변수로 뺀다.
        AdminPostView view = postView(POST_ID, "광고글입니다", "티켓요정", "118.235.13.7", false, null);
        given(adminPostQueryRepository.findAdminViewById(POST_ID)).willReturn(Optional.of(view));

        Page<ReportedTargetRow> result = adminCommunityService.getReportedTargets(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        ReportedTargetRow row = result.getContent().get(0);
        assertThat(row.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(row.getPreview()).isEqualTo("광고글입니다");
        assertThat(row.getNickname()).isEqualTo("티켓요정");
        assertThat(row.getMaskedIp()).isEqualTo("118.235");
        assertThat(row.getReasonCounts()).containsEntry(ReportReason.AD, 2L).containsEntry(ReportReason.ABUSE, 1L);
        assertThat(row.isBlinded()).isFalse();
        assertThat(row.isDeleted()).isFalse();
        assertThat(row.getActionSegment()).isEqualTo("posts");
    }

    @Test
    @DisplayName("신고 대상이 댓글이면 본문 앞부분을 미리보기로 자른다.")
    void reportedTargets_comment_previewTruncated() {
        String longContent = "가".repeat(120);
        PostReportRepository.ReportedTarget target = reportedTarget(ReportTargetType.COMMENT, COMMENT_ID, 1L,
                LocalDateTime.of(2026, 7, 26, 11, 0));
        given(postReportRepository.findReportedTargets(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(target)));
        given(postReportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.COMMENT, COMMENT_ID))
                .willReturn(List.of(report(ReportReason.ETC)));
        AdminCommentView view = commentView(COMMENT_ID, longContent, "익명", "1.2.3.4", false, null);
        given(adminPostCommentQueryRepository.findAdminViewById(COMMENT_ID)).willReturn(Optional.of(view));

        Page<ReportedTargetRow> result = adminCommunityService.getReportedTargets(PageRequest.of(0, 20));

        ReportedTargetRow row = result.getContent().get(0);
        assertThat(row.getTargetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(row.getPreview()).hasSize(81).endsWith("…"); // 80자 + 말줄임표
        assertThat(row.getActionSegment()).isEqualTo("comments");
    }

    @Test
    @DisplayName("신고된 대상을 찾을 수 없으면(이미 삭제) 안내 문구를 담은 행으로 대체한다.")
    void reportedTargets_missingTarget() {
        PostReportRepository.ReportedTarget target = reportedTarget(ReportTargetType.POST, POST_ID, 3L,
                LocalDateTime.now());
        given(postReportRepository.findReportedTargets(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(target)));
        given(postReportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.POST, POST_ID))
                .willReturn(List.of());
        given(adminPostQueryRepository.findAdminViewById(POST_ID)).willReturn(Optional.empty());

        ReportedTargetRow row = adminCommunityService.getReportedTargets(PageRequest.of(0, 20))
                .getContent().get(0);

        assertThat(row.isDeleted()).isTrue();
        assertThat(row.getClientId()).isNull();
        assertThat(row.getPreview()).contains("찾을 수 없");
    }

    // ============ 블라인드 ============

    @Test
    @DisplayName("blindPost는 살아있는 글을 찾아 blind()를 호출한다.")
    void blindPost() {
        Post post = post();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        adminCommunityService.blindPost(POST_ID);

        assertThat(post.getBlinded()).isTrue();
    }

    @Test
    @DisplayName("unblindPost는 블라인드를 해제한다 — 오판을 되돌리는 유일한 경로다.")
    void unblindPost() {
        Post post = post();
        post.blind();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        adminCommunityService.unblindPost(POST_ID);

        assertThat(post.getBlinded()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 글을 블라인드하려 하면 예외를 던진다.")
    void blindPost_notFound() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.blindPost(POST_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("blindComment / unblindComment도 같은 방식으로 동작한다.")
    void blindComment() {
        PostComment comment = comment();
        given(postCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        adminCommunityService.blindComment(COMMENT_ID);
        assertThat(comment.getBlinded()).isTrue();

        adminCommunityService.unblindComment(COMMENT_ID);
        assertThat(comment.getBlinded()).isFalse();
    }

    // ============ 강제 삭제 ============

    @Test
    @DisplayName("deletePost는 소프트 삭제한다 — 영구 보관 원칙(Q9)은 어드민이 지워도 같다.")
    void deletePost() {
        Post post = post();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        adminCommunityService.deletePost(POST_ID);

        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteComment도 같은 방식으로 소프트 삭제한다.")
    void deleteComment() {
        PostComment comment = comment();
        given(postCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        adminCommunityService.deleteComment(COMMENT_ID);

        assertThat(comment.getDeletedAt()).isNotNull();
    }

    // ============ 완전삭제 ============

    @Test
    @DisplayName("purgePost는 삭제된 글의 연관 행까지 모두 지운다 — 신고는 FK가 없어 남으므로 함께 지운다.")
    void purgePost() {
        AdminPostView view = mock(AdminPostView.class);
        given(view.getDeletedAt()).willReturn(LocalDateTime.of(2026, 8, 1, 12, 0));
        given(adminPostQueryRepository.findAdminViewById(POST_ID)).willReturn(Optional.of(view));

        adminCommunityService.purgePost(POST_ID);

        verify(adminCommunityPurgeRepository).deleteCommentReportsOfPost(POST_ID);
        verify(adminCommunityPurgeRepository).deleteReports(ReportTargetType.POST.name(), POST_ID);
        verify(adminCommunityPurgeRepository).deleteLikesOfPost(POST_ID);
        verify(adminCommunityPurgeRepository).deleteTagsOfPost(POST_ID);
        verify(adminCommunityPurgeRepository).deleteCommentsOfPost(POST_ID);
        verify(adminCommunityPurgeRepository).deletePost(POST_ID);
    }

    @Test
    @DisplayName("살아있는 글은 완전삭제하지 않는다 — 삭제와 파기를 2단계로 나눈 이유다.")
    void purgePost_rejectsAlivePost() {
        AdminPostView view = mock(AdminPostView.class);
        given(view.getDeletedAt()).willReturn(null);
        given(adminPostQueryRepository.findAdminViewById(POST_ID)).willReturn(Optional.of(view));

        assertThatThrownBy(() -> adminCommunityService.purgePost(POST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("삭제된 글만");

        verifyNoInteractions(adminCommunityPurgeRepository);
    }

    @Test
    @DisplayName("purgeComment는 댓글과 그 신고 기록만 지운다.")
    void purgeComment() {
        AdminCommentView view = mock(AdminCommentView.class);
        given(view.getDeletedAt()).willReturn(LocalDateTime.of(2026, 8, 1, 12, 0));
        given(adminPostCommentQueryRepository.findAdminViewById(COMMENT_ID)).willReturn(Optional.of(view));

        adminCommunityService.purgeComment(COMMENT_ID);

        verify(adminCommunityPurgeRepository).deleteReports(ReportTargetType.COMMENT.name(), COMMENT_ID);
        verify(adminCommunityPurgeRepository).deleteComment(COMMENT_ID);
    }

    @Test
    @DisplayName("살아있는 댓글도 완전삭제를 거부한다.")
    void purgeComment_rejectsAliveComment() {
        AdminCommentView view = mock(AdminCommentView.class);
        given(view.getDeletedAt()).willReturn(null);
        given(adminPostCommentQueryRepository.findAdminViewById(COMMENT_ID)).willReturn(Optional.of(view));

        assertThatThrownBy(() -> adminCommunityService.purgeComment(COMMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("삭제된 댓글만");

        verifyNoInteractions(adminCommunityPurgeRepository);
    }

    @Test
    @DisplayName("없는 글은 완전삭제할 수 없다.")
    void purgePost_missingPost() {
        given(adminPostQueryRepository.findAdminViewById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.purgePost(POST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 글");
    }

    // ============ 검색 ============

    @Test
    @DisplayName("검색 키워드가 null이면 빈 문자열로 정규화해 넘긴다 — 네이티브 쿼리는 IS NULL이 아니라 빈 값 비교다.")
    void searchPosts_nullKeywordNormalized() {
        given(adminPostQueryRepository.search(eq(""), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        adminCommunityService.searchPosts(null, PageRequest.of(0, 20));

        verify(adminPostQueryRepository).search(eq(""), any(Pageable.class));
    }

    @Test
    @DisplayName("검색 키워드 앞뒤 공백은 지우고 넘긴다.")
    void searchComments_trimsKeyword() {
        given(adminPostCommentQueryRepository.search(eq("공지"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        adminCommunityService.searchComments("  공지  ", PageRequest.of(0, 20));

        verify(adminPostCommentQueryRepository).search(eq("공지"), any(Pageable.class));
    }

    // ============ 밴 ============

    @Test
    @DisplayName("banClient는 기간을 계산해 Client.ban()을 호출한다 — 영구는 until이 null이다.")
    void banClient_permanent() {
        Client client = client();
        given(clientRepository.findById(CLIENT_ID)).willReturn(Optional.of(client));

        adminCommunityService.banClient(CLIENT_ID, BanDuration.PERMANENT, "악질 도배");

        assertThat(client.getBanned()).isTrue();
        assertThat(client.getBannedUntil()).isNull();
        assertThat(client.getBanReason()).isEqualTo("악질 도배");
    }

    @Test
    @DisplayName("1일 밴은 until이 now + 1일이다.")
    void banClient_oneDay() {
        Client client = client();
        given(clientRepository.findById(CLIENT_ID)).willReturn(Optional.of(client));
        LocalDateTime before = LocalDateTime.now();

        adminCommunityService.banClient(CLIENT_ID, BanDuration.ONE_DAY, null);

        assertThat(client.getBannedUntil()).isAfter(before.plusHours(23));
        assertThat(client.getBannedUntil()).isBefore(before.plusDays(1).plusMinutes(1));
    }

    @Test
    @DisplayName("unbanClient는 밴 상태를 완전히 되돌린다 — 잘못 눌렀을 때의 유일한 되돌리기 경로다.")
    void unbanClient() {
        Client client = client();
        client.ban(LocalDateTime.now().plusDays(7), "반복 도배");
        given(clientRepository.findById(CLIENT_ID)).willReturn(Optional.of(client));

        adminCommunityService.unbanClient(CLIENT_ID);

        assertThat(client.getBanned()).isFalse();
        assertThat(client.getBannedUntil()).isNull();
        assertThat(client.getBanReason()).isNull();
    }

    // ── 픽스처 ──

    private PostReportRepository.ReportedTarget reportedTarget(ReportTargetType type, Long id, Long count,
                                                                 LocalDateTime lastReportedAt) {
        PostReportRepository.ReportedTarget target = mock(PostReportRepository.ReportedTarget.class);
        given(target.getTargetType()).willReturn(type);
        given(target.getTargetId()).willReturn(id);
        given(target.getReportCount()).willReturn(count);
        given(target.getLastReportedAt()).willReturn(lastReportedAt);
        return target;
    }

    private PostReport report(ReportReason reason) {
        return PostReport.builder().reason(reason).build();
    }

    private AdminPostView postView(Long id, String title, String nickname, String ip, boolean blinded,
                                    LocalDateTime deletedAt) {
        AdminPostView view = mock(AdminPostView.class);
        given(view.getClientId()).willReturn(CLIENT_ID);
        given(view.getTitle()).willReturn(title);
        given(view.getNickname()).willReturn(nickname);
        given(view.getIp()).willReturn(ip);
        given(view.getBlinded()).willReturn(blinded);
        given(view.getDeletedAt()).willReturn(deletedAt);
        return view;
    }

    private AdminCommentView commentView(Long id, String content, String nickname, String ip, boolean blinded,
                                          LocalDateTime deletedAt) {
        AdminCommentView view = mock(AdminCommentView.class);
        given(view.getClientId()).willReturn(CLIENT_ID);
        given(view.getContent()).willReturn(content);
        given(view.getNickname()).willReturn(nickname);
        given(view.getIp()).willReturn(ip);
        given(view.getBlinded()).willReturn(blinded);
        given(view.getDeletedAt()).willReturn(deletedAt);
        return view;
    }

    private Post post() {
        return Post.builder()
                .id(POST_ID).title("t").content("c").nickname("n").ip("1.2.3.4")
                .deletePasswordHash("h").build();
    }

    private PostComment comment() {
        return PostComment.builder()
                .id(COMMENT_ID).content("c").nickname("n").ip("1.2.3.4").build();
    }

    private Client client() {
        return Client.builder()
                .id(CLIENT_ID).token("tok").ip("1.2.3.4").device("web").referer("-")
                .banned(false).build();
    }
}
