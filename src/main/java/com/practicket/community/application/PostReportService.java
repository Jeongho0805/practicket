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
import com.practicket.community.domain.entity.PostReport;
import com.practicket.community.domain.entity.ReportTargetType;
import com.practicket.community.domain.repository.PostCommentRepository;
import com.practicket.community.domain.repository.PostReportRepository;
import com.practicket.community.domain.repository.PostRepository;
import com.practicket.community.dto.PostReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 자동 블라인드는 운영자가 자는 동안의 보험이다. 주 방어선은 어드민 신고함이다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService {

    /** 낮추면 여러 명이 짜고 멀쩡한 글을 가리기 쉬워진다 */
    private static final int AUTO_BLIND_THRESHOLD = 3;

    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostReportRateLimiter reportRateLimiter;
    private final ClientManager clientManager;

    @Transactional
    public void report(ReportTargetType targetType, Long targetId, PostReportCreateRequest request,
                        ClientInfo clientInfo, String clientIp) {
        validateTargetExists(targetType, targetId);

        // 신고 자체가 어뷰징 수단이 되는 걸 막는다
        reportRateLimiter.validate(clientIp);

        // 기준이 토큰이 아니라 IP 인 이유: 토큰은 무제한으로 새로 발급받을 수 있다
        if (postReportRepository.existsByTargetTypeAndTargetIdAndReporterIp(targetType, targetId, clientIp)) {
            throw new ValidateException(ErrorCode.REPORT_ALREADY_REPORTED);
        }

        Client client = clientManager.findById(clientInfo.getClientId());

        postReportRepository.save(PostReport.builder()
                .targetType(targetType)
                .targetId(targetId)
                .client(client)
                .reporterIp(clientIp)
                .reason(request.getReason())
                .build());

        incrementReportCount(targetType, targetId);
        blindIfThresholdReached(targetType, targetId);
    }

    private void validateTargetExists(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.POST_NOT_FOUND));
            case COMMENT -> postCommentRepository.findById(targetId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));
        }
    }

    private void incrementReportCount(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> postRepository.incrementReportCount(targetId);
            case COMMENT -> postCommentRepository.incrementReportCount(targetId);
        }
    }

    /** 행 수가 아니라 서로 다른 IP 수를 센다 */
    private void blindIfThresholdReached(ReportTargetType targetType, Long targetId) {
        long distinctReporters = postReportRepository.countDistinctReporters(targetType, targetId);
        if (distinctReporters < AUTO_BLIND_THRESHOLD) {
            return;
        }

        switch (targetType) {
            case POST -> postRepository.findById(targetId).ifPresent(Post::blind);
            case COMMENT -> postCommentRepository.findById(targetId).ifPresent(PostComment::blind);
        }
    }
}
