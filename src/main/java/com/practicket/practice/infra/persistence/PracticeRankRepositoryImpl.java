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
                ? "AND (pr.total_duration_ms > :cursorTotalDurationMs OR (pr.total_duration_ms = :cursorTotalDurationMs AND pr.id > :cursorId)) "
                : "";

        // ROW_NUMBER() 윈도우 함수는 sort_buffer_size를 초과하는 대용량 정렬을 유발함.
        // CTE + GROUP BY 방식으로 hash aggregation을 유도해 정렬 메모리 의존을 제거.
        String sql = "WITH best_duration AS ( "
                + "    SELECT client_key, MIN(total_duration_ms) AS min_duration "
                + "    FROM practice_result "
                + "    WHERE type = :type AND started_at >= :startDateTime "
                + "    GROUP BY client_key "
                + "), "
                + "best_row AS ( "
                + "    SELECT MIN(pr.id) AS best_id "
                + "    FROM practice_result pr "
                + "    JOIN best_duration bd ON pr.client_key = bd.client_key "
                + "      AND pr.total_duration_ms = bd.min_duration "
                + "    WHERE pr.type = :type AND pr.started_at >= :startDateTime "
                + "    GROUP BY pr.client_key "
                + ") "
                + "SELECT pr.id, pr.nickname, pr.total_duration_ms "
                + "FROM practice_result pr "
                + "JOIN best_row br ON pr.id = br.best_id "
                + "WHERE 1=1 "
                + cursorClause
                + "ORDER BY pr.total_duration_ms ASC, pr.id ASC "
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
