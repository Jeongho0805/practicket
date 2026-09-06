-- 전체 기간 버킷. 등급과 기록 분포가 전체 기간 기준이라, 그 계산이 원본 practice_result 를
-- 사람별로 묶어야 했다(108만 행 전수 스캔, 실측 5.6초). 기간을 하나 더 두면 사람당 한 줄이라
-- 같은 계산이 idx_pbr_rank 안에서 끝난다.
--
-- period_start 는 전체 기간에 의미가 없지만 NULL 로 두면 유니크 제약이 중복을 못 막는다.
-- MySQL 이 NULL 끼리를 서로 다른 값으로 보기 때문이다. PeriodType.ALL_TIME_START 와 같은 값을 쓴다.

INSERT INTO `practice_best_result`
  (`type`,`period_type`,`period_start`,`client_key`,`nickname`,`result_id`,
   `total_duration_ms`,`reaction_time_ms`,`queue_wait_ms`,`captcha_ms`,`seat_selection_ms`,`updated_at`)
SELECT `type`, 'ALL_TIME', '1970-01-01', `client_key`, `nickname`, `id`,
       `total_duration_ms`, `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`, NOW()
FROM (
  SELECT `id`, `type`, `client_key`, `nickname`, `total_duration_ms`,
         `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`,
         ROW_NUMBER() OVER (PARTITION BY `type`, `client_key`
                            ORDER BY `total_duration_ms`, `id`) AS rn
  FROM `practice_result`
) ranked
WHERE rn = 1;
