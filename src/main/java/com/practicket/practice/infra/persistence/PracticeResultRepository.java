package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PracticeResult;
import com.practicket.practice.domain.PracticeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PracticeResultRepository extends JpaRepository<PracticeResult, Long> {

    // ── 스탯 집계 ──

    @Query("SELECT MIN(pr.totalDurationMs) FROM PracticeResult pr WHERE pr.clientKey = :clientKey AND pr.type = :type")
    Optional<Integer> findBestMs(@Param("clientKey") String clientKey, @Param("type") PracticeType type);

    long countByClientKeyAndType(String clientKey, PracticeType type);

    Optional<PracticeResult> findTopByClientKeyAndTypeOrderByIdAsc(String clientKey, PracticeType type);

    // ── 월간 순위 ──

    @Query("SELECT MIN(pr.totalDurationMs) FROM PracticeResult pr " +
           "WHERE pr.clientKey = :clientKey AND pr.type = :type " +
           "AND pr.startedAt >= :start AND pr.startedAt < :end")
    Optional<Integer> findMonthlyBestMs(@Param("clientKey") String clientKey,
                                        @Param("type") PracticeType type,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // 나보다 좋은 최고기록을 가진 유저 수 — getMyStats 용 (유저별 최고기록 기준)
    @Query(value = "SELECT COUNT(*) FROM (" +
                   "SELECT client_key FROM practice_result " +
                   "WHERE type = :type AND started_at >= :start AND started_at < :end " +
                   "GROUP BY client_key " +
                   "HAVING MIN(total_duration_ms) < :myBestMs) t",
           nativeQuery = true)
    long countUsersWithBetterMonthlyRecord(@Param("type") String type,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("myBestMs") int myBestMs);

    // 하단 고정 줄 용 — 기간 내 내 최고 기록 한 건
    Optional<PracticeResult> findFirstByClientKeyAndTypeAndStartedAtGreaterThanEqualOrderByTotalDurationMsAscIdAsc(
            String clientKey, PracticeType type, LocalDateTime start);

    // 하단 고정 줄 용 — 기간 내 나보다 좋은 최고기록을 가진 유저 수
    @Query(value = "SELECT COUNT(*) FROM (" +
                   "SELECT client_key FROM practice_result " +
                   "WHERE type = :type AND started_at >= :start " +
                   "GROUP BY client_key " +
                   "HAVING MIN(total_duration_ms) < :myBestMs) t",
           nativeQuery = true)
    long countUsersWithBetterRecordSince(@Param("type") String type,
                                         @Param("start") LocalDateTime start,
                                         @Param("myBestMs") int myBestMs);

    @Query(value = "SELECT COUNT(DISTINCT client_key) FROM practice_result " +
                   "WHERE type = :type AND started_at >= :start",
           nativeQuery = true)
    long countUsersSince(@Param("type") String type, @Param("start") LocalDateTime start);

    // 완료 모달 퍼센타일 용 — 기록 전체 기준
    @Query("SELECT COUNT(pr) FROM PracticeResult pr " +
           "WHERE pr.type = :type AND pr.startedAt >= :start AND pr.startedAt < :end " +
           "AND pr.totalDurationMs < :score")
    long countRecordsBetterThan(@Param("type") PracticeType type,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("score") int score);

    @Query("SELECT COUNT(pr) FROM PracticeResult pr " +
           "WHERE pr.type = :type AND pr.startedAt >= :start AND pr.startedAt < :end")
    long countRecordsInMonth(@Param("type") PracticeType type,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    // ── 내 기록 목록 (최신순, cursor = id DESC) ──

    List<PracticeResult> findByClientKeyAndTypeOrderByIdDesc(String clientKey, PracticeType type, Pageable pageable);

    @Query("SELECT pr FROM PracticeResult pr WHERE pr.clientKey = :clientKey AND pr.type = :type AND pr.id < :cursorId ORDER BY pr.id DESC")
    List<PracticeResult> findRecordsBeforeCursor(@Param("clientKey") String clientKey,
                                                 @Param("type") PracticeType type,
                                                 @Param("cursorId") long cursorId,
                                                 Pageable pageable);
}
