package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PracticeRankRepositoryImpl implements PracticeRankRepository {

    private final EntityManager em;

    @Override
    public List<PracticeRankEntry> findRanking(PracticeType type, PeriodType period,
                                               Integer cursorTotalDurationMs, Long cursorId,
                                               int limit) {
        LocalDateTime startDateTime = period.getStartDateTime();

        String cursorClause = cursorTotalDurationMs != null
                ? "AND (total_duration_ms > :cursorTotalDurationMs OR (total_duration_ms = :cursorTotalDurationMs AND id > :cursorId)) "
                : "";

        String sql = "SELECT id, nickname, total_duration_ms "
                + "FROM ( "
                + "    SELECT id, nickname, total_duration_ms, "
                + "           ROW_NUMBER() OVER (PARTITION BY client_key ORDER BY total_duration_ms ASC, id ASC) AS rn "
                + "    FROM practice_result "
                + "    WHERE type = :type "
                + "      AND created_at >= :startDateTime "
                + ") ranked "
                + "WHERE rn = 1 "
                + cursorClause
                + "ORDER BY total_duration_ms ASC, id ASC "
                + "LIMIT :limit";

        var query = em.createNativeQuery(sql)
                .setParameter("type", type.name())
                .setParameter("startDateTime", startDateTime)
                .setParameter("limit", limit);

        if (cursorTotalDurationMs != null) {
            query.setParameter("cursorTotalDurationMs", cursorTotalDurationMs);
            query.setParameter("cursorId", cursorId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> new PracticeRankEntry(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).intValue()
                ))
                .toList();
    }
}
