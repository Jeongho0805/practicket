-- 보안문자 구간 신설. docs/practice-record-integrity.md 6항.
--
-- 지금까지 보안문자 입력 시간은 i-ticket·n-ticket 에서는 좌석 구간에 섞여 있었고
-- m-ticket 에서는 어느 구간에도 잡히지 않아 기록에서 통째로 빠져 있었다.
-- 구간을 하나 떼어내 세 화면의 기준을 맞춘다.
--
-- 기존 행은 0 으로 둔다. 지난 기록에는 이 값을 복원할 근거가 없다.
ALTER TABLE `practice_result`
  ADD COLUMN `captcha_ms` int NOT NULL DEFAULT 0 AFTER `queue_wait_ms`;
