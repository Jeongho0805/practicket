package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PracticeBestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PracticeBestResultRepository
        extends JpaRepository<PracticeBestResult, Long>, PracticeBestResultRepositoryCustom {

    /**
     * 네 기간의 자리를 잡는다. 이미 있으면 아무것도 하지 않는다 — 값 판단은 뒤따르는
     * {@code updateBucketsIfFaster} 가 한다.
     * <p>
     * "찾아보고 없으면 넣기" 로 하면 같은 사람이 동시에 두 기록을 끝냈을 때 둘 다 넣으려다
     * 하나가 유니크 제약에 걸려 그 요청의 연습 기록까지 롤백된다. 그 경합을 DB 에 맡긴다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO practice_best_result
                (type, period_type, period_start, client_key, nickname, result_id,
                 total_duration_ms, reaction_time_ms, queue_wait_ms, captcha_ms, seat_selection_ms, updated_at)
            VALUES
                (:type, 'DAILY',    :dailyStart,   :clientKey, :nickname, :resultId,
                 :totalDurationMs, :reactionTimeMs, :queueWaitMs, :captchaMs, :seatSelectionMs, NOW()),
                (:type, 'WEEKLY',   :weeklyStart,  :clientKey, :nickname, :resultId,
                 :totalDurationMs, :reactionTimeMs, :queueWaitMs, :captchaMs, :seatSelectionMs, NOW()),
                (:type, 'MONTHLY',  :monthlyStart, :clientKey, :nickname, :resultId,
                 :totalDurationMs, :reactionTimeMs, :queueWaitMs, :captchaMs, :seatSelectionMs, NOW()),
                (:type, 'ALL_TIME', :allTimeStart, :clientKey, :nickname, :resultId,
                 :totalDurationMs, :reactionTimeMs, :queueWaitMs, :captchaMs, :seatSelectionMs, NOW())
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void insertBucketsIfAbsent(@Param("type") String type,
                               @Param("dailyStart") LocalDate dailyStart,
                               @Param("weeklyStart") LocalDate weeklyStart,
                               @Param("monthlyStart") LocalDate monthlyStart,
                               @Param("allTimeStart") LocalDate allTimeStart,
                               @Param("clientKey") String clientKey,
                               @Param("nickname") String nickname,
                               @Param("resultId") Long resultId,
                               @Param("totalDurationMs") int totalDurationMs,
                               @Param("reactionTimeMs") int reactionTimeMs,
                               @Param("queueWaitMs") int queueWaitMs,
                               @Param("captchaMs") int captchaMs,
                               @Param("seatSelectionMs") int seatSelectionMs);

    /** 마지막 조건 한 줄이 "최고기록일 때만" 이다. DB 가 줄을 잠근 채 비교하므로 경합해도 느린 기록이 남지 않는다. */
    @Modifying
    @Query(value = """
            UPDATE practice_best_result
               SET nickname = :nickname,
                   result_id = :resultId,
                   reaction_time_ms = :reactionTimeMs,
                   queue_wait_ms = :queueWaitMs,
                   captcha_ms = :captchaMs,
                   seat_selection_ms = :seatSelectionMs,
                   total_duration_ms = :totalDurationMs,
                   updated_at = NOW()
             WHERE client_key = :clientKey
               AND type = :type
               AND ((period_type = 'DAILY'    AND period_start = :dailyStart)
                 OR (period_type = 'WEEKLY'   AND period_start = :weeklyStart)
                 OR (period_type = 'MONTHLY'  AND period_start = :monthlyStart)
                 OR (period_type = 'ALL_TIME' AND period_start = :allTimeStart))
               AND total_duration_ms > :totalDurationMs
            """, nativeQuery = true)
    void updateBucketsIfFaster(@Param("type") String type,
                               @Param("dailyStart") LocalDate dailyStart,
                               @Param("weeklyStart") LocalDate weeklyStart,
                               @Param("monthlyStart") LocalDate monthlyStart,
                               @Param("allTimeStart") LocalDate allTimeStart,
                               @Param("clientKey") String clientKey,
                               @Param("nickname") String nickname,
                               @Param("resultId") Long resultId,
                               @Param("totalDurationMs") int totalDurationMs,
                               @Param("reactionTimeMs") int reactionTimeMs,
                               @Param("queueWaitMs") int queueWaitMs,
                               @Param("captchaMs") int captchaMs,
                               @Param("seatSelectionMs") int seatSelectionMs);
}
