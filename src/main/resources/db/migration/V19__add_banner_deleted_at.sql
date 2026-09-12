-- 배너 소프트 삭제. 캠페인 수정에서 배너를 빼도 행은 남겨 광고주 리포트의 노출·클릭 합계가 줄지 않게 한다.
-- NULL 이면 살아 있는 배너다.
ALTER TABLE `banner`
  ADD COLUMN `deleted_at` datetime DEFAULT NULL;
