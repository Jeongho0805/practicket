-- 네트워크별 재요청 간격과 대타 네트워크. 같은 브라우저 탭에서 간격 안에 다시 열린 자리는
-- 첫째 네트워크 대신 대타로 채운다. 간격이 NULL 이면 지금처럼 제한 없음.
ALTER TABLE `ad_network_exposure`
  ADD COLUMN `refill_gap_minutes` int DEFAULT NULL,
  ADD COLUMN `fallback_network` varchar(32) DEFAULT NULL;

UPDATE `ad_network_exposure`
   SET `refill_gap_minutes` = 5, `fallback_network` = 'ADFIT'
 WHERE `network` = 'ADSENSE' AND `refill_gap_minutes` IS NULL;
