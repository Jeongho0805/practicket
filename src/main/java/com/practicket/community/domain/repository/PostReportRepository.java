package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.PostReport;
import com.practicket.community.domain.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByTargetTypeAndTargetIdAndReporterIp(ReportTargetType targetType, Long targetId, String reporterIp);

    /** 행 수가 아니라 서로 다른 IP 수를 센다 */
    @Query("""
            SELECT COUNT(DISTINCT r.reporterIp)
            FROM PostReport r
            WHERE r.targetType = :targetType AND r.targetId = :targetId
            """)
    long countDistinctReporters(@Param("targetType") ReportTargetType targetType,
                                @Param("targetId") Long targetId);

    /** IP 당 하루 신고 수 */
    long countByReporterIpAndCreatedAtAfter(String reporterIp, java.time.LocalDateTime since);

    /** 대상별로 묶어 누적 많은 순. 임계치와 무관하게 한 건이라도 들어오면 보여준다 */
    @Query("""
            SELECT r.targetType AS targetType,
                   r.targetId AS targetId,
                   COUNT(DISTINCT r.reporterIp) AS reportCount,
                   MAX(r.createdAt) AS lastReportedAt
            FROM PostReport r
            GROUP BY r.targetType, r.targetId
            ORDER BY COUNT(DISTINCT r.reporterIp) DESC, MAX(r.createdAt) DESC
            """)
    Page<ReportedTarget> findReportedTargets(Pageable pageable);

    List<PostReport> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType targetType, Long targetId);

    /** 신고함 한 줄 */
    interface ReportedTarget {
        ReportTargetType getTargetType();

        Long getTargetId();

        Long getReportCount();

        java.time.LocalDateTime getLastReportedAt();
    }
}
