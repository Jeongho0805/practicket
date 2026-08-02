-- prod 와 stage 의 인덱스가 서로 어긋나 있던 것을 맞춘다 (2026-07-26 확인).
--
--   captcha_result   idx_captcha_elapsed / idx_captcha_client_created  → stage 에만 없음
--   client           idx_client_token                                  → stage 에만 없음
--                    (매 요청 findByToken 이 stage 에서 풀스캔이었다)
--   practice_result  idx_pr_type_started_dur                           → prod 에만 있음
--                    idx_practice_result_type_started_at_total_duration 와 컬럼이 완전히 같은
--                    중복 인덱스. 조회엔 도움이 없고 INSERT 마다 두 번 갱신된다.
--
-- MySQL 은 인덱스에 IF NOT EXISTS 를 지원하지 않는다. 환경마다 있고 없는 것이 달라
-- 그냥 CREATE/DROP 하면 한쪽에서 반드시 실패하므로, information_schema 를 보고
-- 필요할 때만 실행한다. 이 지저분한 방식은 드리프트를 정리하는 이번 한 번만 쓴다.

-- captcha_result.idx_captcha_elapsed
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'captcha_result'
                 AND index_name = 'idx_captcha_elapsed');
SET @ddl := IF(@exist = 0,
    'CREATE INDEX idx_captcha_elapsed ON captcha_result (elapsed_second)',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- captcha_result.idx_captcha_client_created
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'captcha_result'
                 AND index_name = 'idx_captcha_client_created');
SET @ddl := IF(@exist = 0,
    'CREATE INDEX idx_captcha_client_created ON captcha_result (client_id, created_at)',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- client.idx_client_token
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'client'
                 AND index_name = 'idx_client_token');
SET @ddl := IF(@exist = 0,
    'CREATE INDEX idx_client_token ON client (token)',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- practice_result.idx_pr_type_started_dur (중복 → 제거)
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'practice_result'
                 AND index_name = 'idx_pr_type_started_dur');
SET @ddl := IF(@exist > 0,
    'DROP INDEX idx_pr_type_started_dur ON practice_result',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
