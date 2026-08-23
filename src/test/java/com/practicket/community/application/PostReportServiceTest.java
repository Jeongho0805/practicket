package com.practicket.community.application;

import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import com.practicket.common.exception.ValidateException;
import com.practicket.community.component.PostReportRateLimiter;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import com.practicket.community.domain.entity.ReportReason;
import com.practicket.community.domain.entity.ReportTargetType;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostReportRepository;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.PostReportCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    private static final Long CLIENT_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;
    private static final String CLIENT_IP = "118.235.13.7";

    @InjectMocks
    private PostReportService postReportService;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostReportRateLimiter reportRateLimiter;

    @Mock
    private ClientManager clientManager;

    @Test
    @DisplayName("report - 존재하지 않는 글은 신고할 수 없다")
    void reportRejectsMissingPost() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postReportService.report(
                ReportTargetType.POST, POST_ID, reportRequest(ReportReason.AD), clientInfo(), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postReportRepository, never()).save(any());
        verifyNoInteractions(reportRateLimiter);
    }

    @Test
    @DisplayName("report - 존재하지 않는 댓글은 신고할 수 없다")
    void reportRejectsMissingComment() {
        // given
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postReportService.report(
                ReportTargetType.COMMENT, COMMENT_ID, reportRequest(ReportReason.AD), clientInfo(), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        verify(postReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("report - 레이트리밋에 걸리면 저장하지 않고 그대로 전파한다")
    void reportPropagatesRateLimitException() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        doThrow(new GlobalException(ErrorCode.REPORT_RATE_LIMIT_IP))
                .when(reportRateLimiter).validate(anyString());

        // when & then
        assertThatThrownBy(() -> postReportService.report(
                ReportTargetType.POST, POST_ID, reportRequest(ReportReason.AD), clientInfo(), CLIENT_IP))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_RATE_LIMIT_IP);

        verify(postReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("report - 같은 IP 로 같은 대상을 또 신고하면 막는다")
    void reportRejectsDuplicateReportFromSameIp() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postReportRepository.existsByTargetTypeAndTargetIdAndReporterIp(
                ReportTargetType.POST, POST_ID, CLIENT_IP)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> postReportService.report(
                ReportTargetType.POST, POST_ID, reportRequest(ReportReason.AD), clientInfo(), CLIENT_IP))
                .isInstanceOf(ValidateException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_ALREADY_REPORTED);

        verify(postReportRepository, never()).save(any());
        verify(postRepository, never()).incrementReportCount(any());
    }

    @Test
    @DisplayName("report - 정상 신고는 저장되고 대상의 신고 수가 오른다")
    void reportSavesAndIncrementsCount() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        givenClient();
        // 아직 임계치(3) 미만이라 블라인드되지 않는다
        when(postReportRepository.countDistinctReporters(ReportTargetType.POST, POST_ID)).thenReturn(1L);

        // when
        postReportService.report(ReportTargetType.POST, POST_ID, reportRequest(ReportReason.ABUSE), clientInfo(), CLIENT_IP);

        // then
        verify(postReportRepository).save(any());
        verify(postRepository).incrementReportCount(POST_ID);
    }

    @Test
    @DisplayName("report - 서로 다른 IP 3개 미만이면 블라인드하지 않는다")
    void reportDoesNotBlindBelowThreshold() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        givenClient();
        when(postReportRepository.countDistinctReporters(ReportTargetType.POST, POST_ID)).thenReturn(2L);

        // when
        postReportService.report(ReportTargetType.POST, POST_ID, reportRequest(ReportReason.ABUSE), clientInfo(), CLIENT_IP);

        // then
        assertThat(post.getBlinded()).isFalse();
    }

    @Test
    @DisplayName("report - 서로 다른 IP 3개가 모이면 글을 블라인드한다")
    void reportBlindsPostAtThreshold() {
        // given
        Post post = post();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        givenClient();
        when(postReportRepository.countDistinctReporters(ReportTargetType.POST, POST_ID)).thenReturn(3L);

        // when
        postReportService.report(ReportTargetType.POST, POST_ID, reportRequest(ReportReason.ABUSE), clientInfo(), CLIENT_IP);

        // then
        assertThat(post.getBlinded()).isTrue();
    }

    @Test
    @DisplayName("report - 서로 다른 IP 3개가 모이면 댓글도 블라인드한다")
    void reportBlindsCommentAtThreshold() {
        // given
        PostComment comment = comment();
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        givenClient();
        when(postReportRepository.countDistinctReporters(ReportTargetType.COMMENT, COMMENT_ID)).thenReturn(3L);

        // when
        postReportService.report(ReportTargetType.COMMENT, COMMENT_ID, reportRequest(ReportReason.PRIVACY), clientInfo(), CLIENT_IP);

        // then
        assertThat(comment.getBlinded()).isTrue();
        verify(postCommentRepository).incrementReportCount(COMMENT_ID);
    }

    // ============ helpers ============

    private void givenClient() {
        when(clientManager.findById(CLIENT_ID)).thenReturn(client());
    }

    private Client client() {
        return Client.builder()
                .id(CLIENT_ID)
                .token("token-" + CLIENT_ID)
                .ip(CLIENT_IP)
                .device("device")
                .referer("referer")
                .name("구경꾼")
                .banned(false)
                .build();
    }

    private Post post() {
        return Post.builder()
                .id(POST_ID)
                .client(client())
                .title("제목")
                .content("본문")
                .nickname("티켓요정")
                .ip(CLIENT_IP)
                .deletePasswordHash("$2a$hashed")
                .build();
    }

    private PostComment comment() {
        return PostComment.builder()
                .id(COMMENT_ID)
                .post(post())
                .client(client())
                .content("내용")
                .nickname("티켓요정")
                .ip(CLIENT_IP)
                .build();
    }

    private ClientInfo clientInfo() {
        return ClientInfo.builder()
                .clientId(CLIENT_ID)
                .token("token-" + CLIENT_ID)
                .banned(false)
                .build();
    }

    private PostReportCreateRequest reportRequest(ReportReason reason) {
        PostReportCreateRequest request = new PostReportCreateRequest();
        request.setReason(reason);
        return request;
    }
}
