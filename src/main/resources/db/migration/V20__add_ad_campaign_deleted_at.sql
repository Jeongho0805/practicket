-- 캠페인 소프트 삭제. 배너와 같은 방식으로 행은 남기고 목록·노출에서만 뺀다.
ALTER TABLE `ad_campaign`
  ADD COLUMN `deleted_at` datetime DEFAULT NULL;
