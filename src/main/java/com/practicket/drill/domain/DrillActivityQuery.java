package com.practicket.drill.domain;

import com.practicket.drill.dto.DrillActivityResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 네 종목의 판 기록을 이어 붙인 뒤, 사람 수 중복 제거는 바깥에서 한 번만 한다. 여러 종목을 한 사람도 1명이다. */
@Repository
@RequiredArgsConstructor
public class DrillActivityQuery {

    private static final String SQL = """
            SELECT
              COUNT(DISTINCT client_id),
              COUNT(*),
              COUNT(DISTINCT CASE WHEN drill = 'REACT' THEN client_id END),
              SUM(CASE WHEN drill = 'REACT' THEN 1 ELSE 0 END),
              COUNT(DISTINCT CASE WHEN drill = 'CAPTCHA' THEN client_id END),
              SUM(CASE WHEN drill = 'CAPTCHA' THEN 1 ELSE 0 END),
              COUNT(DISTINCT CASE WHEN drill = 'GRAPE' THEN client_id END),
              SUM(CASE WHEN drill = 'GRAPE' THEN 1 ELSE 0 END),
              COUNT(DISTINCT CASE WHEN drill = 'CANCEL' THEN client_id END),
              SUM(CASE WHEN drill = 'CANCEL' THEN 1 ELSE 0 END)
            FROM (
              SELECT 'REACT' AS drill, client_id FROM drill_react_result WHERE created_at >= :from
              UNION ALL
              SELECT 'CAPTCHA' AS drill, client_id FROM drill_captcha_result WHERE created_at >= :from
              UNION ALL
              SELECT 'GRAPE' AS drill, client_id FROM drill_grape_result WHERE created_at >= :from
              UNION ALL
              SELECT 'CANCEL' AS drill, client_id FROM drill_cancel_result WHERE created_at >= :from
            ) t
            """;

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DrillActivityResponse since(LocalDateTime from) {
        Object[] row = (Object[]) entityManager.createNativeQuery(SQL).setParameter("from", from).getSingleResult();
        return new DrillActivityResponse(
                number(row[0]), number(row[1]),
                new DrillActivityResponse.Drill(number(row[2]), number(row[3])),
                new DrillActivityResponse.Drill(number(row[4]), number(row[5])),
                new DrillActivityResponse.Drill(number(row[6]), number(row[7])),
                new DrillActivityResponse.Drill(number(row[8]), number(row[9])));
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
