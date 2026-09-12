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
