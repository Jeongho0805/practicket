-- 스테이지에서 외부 네트워크 광고를 실제로 확인할 때 쓰는 네트워크별 스위치.
-- 운영은 항상 허용하고 로컬은 항상 차단하므로 이 값은 stage 프로필에서만 읽는다.
CREATE TABLE IF NOT EXISTS `ad_network_exposure` (
  `network` varchar(32) NOT NULL,
  `stage_enabled` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`network`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `ad_network_exposure` (`network`, `stage_enabled`) VALUES
  ('COUPANG', 1),
  ('ADSENSE', 0),
  ('ADFIT', 0);
