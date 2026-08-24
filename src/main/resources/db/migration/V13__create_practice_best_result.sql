-- 기간별 최고 기록. 랭킹 조회를 읽기 시점 계산에서 쓰기 시점 적재로 옮긴다.
--
-- practice_result 는 시도마다 한 행이 쌓이는데(I_TICKET_OLD 만 107만 행) 랭킹은
-- "사람마다 최고 기록 하나"라, 조회할 때마다 그 달 전체를 사람별로 묶어 다시 줄 세우고 있었다.
-- 커서 페이지네이션도 그 계산 바깥에 붙어 있어 스크롤을 내릴수록 같은 일을 반복했다.
-- 이 테이블은 그 결과를 미리 한 줄로 들고 있어, 조회가 인덱스 순서대로 집어오기만 하면 된다.

CREATE TABLE `practice_best_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  `type` varchar(30) NOT NULL,

  -- DAILY | WEEKLY | MONTHLY
  `period_type` varchar(10) NOT NULL,
  -- 그 기간의 첫날. 주는 월요일, 월은 1일이다
  `period_start` date NOT NULL,

  `client_key` varchar(255) NOT NULL,
  `nickname` varchar(255) NOT NULL,

  -- 이 최고 기록의 원본 practice_result.id. 동점 순서를 원본과 같게 유지하고,
  -- 값이 어긋났을 때 되짚을 수 있게 들고 있는다
  `result_id` bigint NOT NULL,

  `total_duration_ms` int NOT NULL,
  `reaction_time_ms` int NOT NULL,
  `queue_wait_ms` int NOT NULL,
  `captcha_ms` int NOT NULL,
  `seat_selection_ms` int NOT NULL,

  `updated_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  -- 기록 저장 시 덮어쓸 줄을 찾는 키. 사람당 기간당 한 줄이라는 제약 자체이기도 하다.
  -- client_key 를 앞에 두는 이유는 아래 랭킹 인덱스와 선두 3컬럼이 겹치지 않게 하기 위해서다.
  -- 겹치면 옵티마이저가 등수 집계에서 이쪽을 골라 원본까지 되짚는다
  UNIQUE KEY `uk_pbr_client_bucket` (`client_key`,`type`,`period_type`,`period_start`),
  -- 랭킹 목록·커서 이어보기·내 등수 세기를 모두 이 하나로 처리한다.
  -- 컬럼 순서가 조회의 ORDER BY 와 같아야 정렬 없이 앞에서부터 집어올 수 있다
  KEY `idx_pbr_rank` (`type`,`period_type`,`period_start`,`total_duration_ms`,`result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 지금 화면에 보이는 세 기간만 채운다. 지난 기간 랭킹은 조회할 경로가 없어 채워도 쓰이지 않는다.
-- 이후 기간은 기록이 저장될 때마다 애플리케이션이 채운다.

INSERT INTO `practice_best_result`
  (`type`,`period_type`,`period_start`,`client_key`,`nickname`,`result_id`,
   `total_duration_ms`,`reaction_time_ms`,`queue_wait_ms`,`captcha_ms`,`seat_selection_ms`,`updated_at`)
SELECT `type`, 'MONTHLY', DATE_FORMAT(`started_at`, '%Y-%m-01'), `client_key`, `nickname`, `id`,
       `total_duration_ms`, `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`, NOW()
FROM (
  SELECT `id`, `type`, `client_key`, `nickname`, `started_at`, `total_duration_ms`,
         `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`,
         ROW_NUMBER() OVER (PARTITION BY `type`, `client_key`
                            ORDER BY `total_duration_ms`, `id`) AS rn
  FROM `practice_result`
  WHERE `started_at` >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
) ranked
WHERE rn = 1;

-- WEEKDAY() 는 월요일이 0 이라, 그만큼 빼면 그 주의 월요일이 된다.
-- 애플리케이션의 주 시작(DayOfWeek.MONDAY)과 같은 기준이어야 한다
INSERT INTO `practice_best_result`
  (`type`,`period_type`,`period_start`,`client_key`,`nickname`,`result_id`,
   `total_duration_ms`,`reaction_time_ms`,`queue_wait_ms`,`captcha_ms`,`seat_selection_ms`,`updated_at`)
SELECT `type`, 'WEEKLY', DATE_SUB(DATE(`started_at`), INTERVAL WEEKDAY(`started_at`) DAY),
       `client_key`, `nickname`, `id`,
       `total_duration_ms`, `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`, NOW()
FROM (
  SELECT `id`, `type`, `client_key`, `nickname`, `started_at`, `total_duration_ms`,
         `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`,
         ROW_NUMBER() OVER (PARTITION BY `type`, `client_key`
                            ORDER BY `total_duration_ms`, `id`) AS rn
  FROM `practice_result`
  WHERE `started_at` >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
) ranked
WHERE rn = 1;

INSERT INTO `practice_best_result`
  (`type`,`period_type`,`period_start`,`client_key`,`nickname`,`result_id`,
   `total_duration_ms`,`reaction_time_ms`,`queue_wait_ms`,`captcha_ms`,`seat_selection_ms`,`updated_at`)
SELECT `type`, 'DAILY', DATE(`started_at`), `client_key`, `nickname`, `id`,
       `total_duration_ms`, `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`, NOW()
FROM (
  SELECT `id`, `type`, `client_key`, `nickname`, `started_at`, `total_duration_ms`,
         `reaction_time_ms`, `queue_wait_ms`, `captcha_ms`, `seat_selection_ms`,
         ROW_NUMBER() OVER (PARTITION BY `type`, `client_key`
                            ORDER BY `total_duration_ms`, `id`) AS rn
  FROM `practice_result`
  WHERE `started_at` >= CURDATE()
) ranked
WHERE rn = 1;
