package com.practicket.community.admin;

import com.practicket.client.domain.Client;
import com.practicket.client.domain.ClientRepository;
import com.practicket.community.admin.dto.AdminCommentView;
import com.practicket.community.admin.dto.AdminPostView;
import com.practicket.community.admin.dto.BanDuration;
import com.practicket.community.admin.dto.ReportedTargetRow;
import com.practicket.community.admin.repository.AdminPostCommentQueryRepository;
import com.practicket.community.admin.repository.AdminPostQueryRepository;
import com.practicket.community.component.IpMasker;
import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostComment;
import com.practicket.community.domain.entity.PostReport;
import com.practicket.community.domain.entity.ReportReason;
import com.practicket.community.domain.entity.ReportTargetType;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostReportRepository;
import com.practicket.community.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 신고 대상은 이미 삭제됐을 수 있다 — {@code post_report} 는 FK 가 아니라 target_type+target_id 조합이라
 * 대상이 사라져도 정리되지 않는다. 그래서 조회는 어드민 전용 프로젝션으로, 상태 변경은 살아있는 것에만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommunityService {

    /** 신고함 목록의 본문 미리보기 길이 */
    private static final int PREVIEW_LENGTH = 80;

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostReportRepository postReportRepository;
    private final AdminPostQueryRepository adminPostQueryRepository;
    private final AdminPostCommentQueryRepository adminPostCommentQueryRepository;
    private final ClientRepository clientRepository;

    // ── 신고함 ──

    @Transactional(readOnly = true)
    public Page<ReportedTargetRow> getReportedTargets(Pageable pageable) {
        return postReportRepository.findReportedTargets(pageable).map(this::toReportedTargetRow);
    }

    private ReportedTargetRow toReportedTargetRow(PostReportRepository.ReportedTarget target) {
        Map<ReportReason, Long> reasonCounts = reasonCounts(target.getTargetType(), target.getTargetId());

        if (target.getTargetType() == ReportTargetType.POST) {
            return adminPostQueryRepository.findAdminViewById(target.getTargetId())
                    .map(view -> new ReportedTargetRow(
                            ReportTargetType.POST, target.getTargetId(), target.getReportCount(),
                            target.getLastReportedAt(), reasonCounts,
                            view.getTitle(), view.getNickname(), IpMasker.maskToTwoSegments(view.getIp()),
                            view.getClientId(), Boolean.TRUE.equals(view.getBlinded()), view.getDeletedAt() != null))
                    .orElseGet(() -> missingTargetRow(ReportTargetType.POST, target, reasonCounts));
        }

        return adminPostCommentQueryRepository.findAdminViewById(target.getTargetId())
                .map(view -> new ReportedTargetRow(
                        ReportTargetType.COMMENT, target.getTargetId(), target.getReportCount(),
                        target.getLastReportedAt(), reasonCounts,
                        preview(view.getContent()), view.getNickname(), IpMasker.maskToTwoSegments(view.getIp()),
                        view.getClientId(), Boolean.TRUE.equals(view.getBlinded()), view.getDeletedAt() != null))
                .orElseGet(() -> missingTargetRow(ReportTargetType.COMMENT, target, reasonCounts));
    }

    /** soft delete 라 행은 남지만 조회가 비는 경우까지 방어한다 */
    private ReportedTargetRow missingTargetRow(ReportTargetType type, PostReportRepository.ReportedTarget target,
                                                Map<ReportReason, Long> reasonCounts) {
        return new ReportedTargetRow(type, target.getTargetId(), target.getReportCount(),
                target.getLastReportedAt(), reasonCounts, "(대상을 찾을 수 없습니다)", "-", "-",
                null, false, true);
    }

    /** 대상당 신고 수가 적어 N+1 이어도 부담이 없다 */
    private Map<ReportReason, Long> reasonCounts(ReportTargetType type, Long targetId) {
        Map<ReportReason, Long> counts = new EnumMap<>(ReportReason.class);
        List<PostReport> reports = postReportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId);
        for (PostReport report : reports) {
            counts.merge(report.getReason(), 1L, Long::sum);
        }
        return counts;
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        return trimmed.length() > PREVIEW_LENGTH ? trimmed.substring(0, PREVIEW_LENGTH) + "…" : trimmed;
    }

    // ── 블라인드 ──

    public void blindPost(Long id) {
        post(id).blind();
    }

    public void unblindPost(Long id) {
        post(id).unblind();
    }

    public void blindComment(Long id) {
        comment(id).blind();
    }

    public void unblindComment(Long id) {
        comment(id).unblind();
    }

    // ── 강제 삭제 ──

    public void deletePost(Long id) {
        post(id).softDelete(LocalDateTime.now());
    }

    public void deleteComment(Long id) {
        comment(id).softDelete(LocalDateTime.now());
    }

    private Post post(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 삭제된 글입니다. id=" + id));
    }

    private PostComment comment(Long id) {
        return postCommentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 삭제된 댓글입니다. id=" + id));
    }

    // ── 글·댓글 관리 화면 ──

    @Transactional(readOnly = true)
    public Page<AdminPostView> searchPosts(String keyword, Pageable pageable) {
        return adminPostQueryRepository.search(normalize(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminCommentView> searchComments(String keyword, Pageable pageable) {
        return adminPostCommentQueryRepository.search(normalize(keyword), pageable);
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.strip();
    }

    // ── 밴 ──

    /** 서버는 판단하지 않고 운영자가 고른 기간을 그대로 적용한다 */
    public void banClient(Long clientId, BanDuration duration, String reason) {
        Client client = findClient(clientId);
        client.ban(duration.resolveUntil(LocalDateTime.now()), reason);
    }

    /** 잘못 눌렀을 때 되돌리는 경로 */
    public void unbanClient(Long clientId) {
        findClient(clientId).unban();
    }

    private Client findClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + clientId));
    }
}
