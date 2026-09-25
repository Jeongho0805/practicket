-- 모비센스 지면은 번호 말고도 frameCode·settings 가 지면마다 달라 광고 코드에 그대로 들어가야 한다.
-- 네트워크 전용 값을 JSON 한 줄로 단위에 같이 둔다. 애드센스·쿠팡·애드핏은 비운다.
ALTER TABLE `ad_unit` ADD COLUMN `extra` varchar(500) DEFAULT NULL;

INSERT IGNORE INTO `ad_network_exposure` (`network`, `stage_enabled`) VALUES ('MOBSENSE', 0);
