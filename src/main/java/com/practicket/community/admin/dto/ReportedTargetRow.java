package com.practicket.community.admin.dto;

import com.practicket.community.domain.entity.ReportReason;
import com.practicket.community.domain.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/** FK 가 없어 신고 집계와 대상 조회를 서비스에서 직접 조립한다 */
@Getter
@AllArgsConstructor
public class ReportedTargetRow {

    private final ReportTargetType targetType;
    private final Long targetId;
    private final long reportCount;
    private final LocalDateTime lastReportedAt;
    private final Map<ReportReason, Long> reasonCounts;
    /** 글이면 제목, 댓글이면 본문 앞부분. 삭제됐으면 안내 문구 */
    private final String preview;
    private final String nickname;
    private final String maskedIp;
    /** 작성자 밴 버튼에 쓴다. 대상 조회 실패 시에만 null */
    private final Long clientId;
    private final boolean blinded;
    private final boolean deleted;

    public String getTargetLabel() {
        return targetType == ReportTargetType.POST ? "글" : "댓글";
    }

    /** 템플릿이 URL 을 조립할 때 쓰는 경로 조각(posts/comments) */
    public String getActionSegment() {
        return targetType == ReportTargetType.POST ? "posts" : "comments";
    }
}
