-- 자리의 채움 순서 중 2단계부터. 1단계는 ad_slot 의 fill_network·pc_ad_unit_id·mobile_ad_unit_id 다.
-- 브라우저는 위 단계부터 보며 재요청 간격에 걸린 네트워크를 건너뛴다. 단위가 NULL 이면 규격으로 자동 선택.
CREATE TABLE IF NOT EXISTS `ad_slot_fill_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot_id` bigint NOT NULL,
  `step_order` int NOT NULL,
  `network` varchar(32) NOT NULL,
  `pc_ad_unit_id` bigint DEFAULT NULL,
  `mobile_ad_unit_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ad_slot_fill_step_slot_id` (`slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 네트워크 단위 대타(ad_network_exposure.fallback_network)를 쓰던 자리를 2단계로 옮긴다.
-- 옛 칸은 이전 버전으로 되돌릴 때를 위해 남긴다.
INSERT INTO `ad_slot_fill_step` (`slot_id`, `step_order`, `network`)
SELECT s.`id`, 2, e.`fallback_network`
  FROM `ad_slot` s
  JOIN `ad_network_exposure` e ON e.`network` = s.`fill_network`
 WHERE e.`fallback_network` IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM `ad_slot_fill_step` f WHERE f.`slot_id` = s.`id`);
