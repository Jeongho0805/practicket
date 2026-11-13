-- 재요청 간격을 초 단위로 바꾼다. refill_gap_minutes 는 이전 버전으로 되돌려도 동작하게 남겨 두고
-- 코드는 더 읽지 않는다. 안정되면 fallback_network 와 함께 별도 마이그레이션으로 지운다.
ALTER TABLE `ad_network_exposure`
  ADD COLUMN `refill_gap_seconds` int DEFAULT NULL;

UPDATE `ad_network_exposure`
   SET `refill_gap_seconds` = `refill_gap_minutes` * 60
 WHERE `refill_gap_minutes` IS NOT NULL;
