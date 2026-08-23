-- 커뮤니티 신고. 설계는 docs/community-system.md Q7.
--
-- 글과 댓글을 한 테이블에서 받는다. 테이블을 둘로 쪼개면 어드민 신고함이
-- 두 곳을 합쳐 정렬해야 하는데, 신고는 "누적 순으로 한 줄에 놓고 보는" 화면이 전부다.
-- 그래서 FK 대신 target_type + target_id 조합으로 가리킨다.

CREATE TABLE `post_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  -- POST | COMMENT
  `target_type` varchar(255) NOT NULL,
  `target_id` bigint NOT NULL,

  `client_id` bigint NOT NULL,
  -- 중복 방지 기준은 토큰이 아니라 IP 다. 토큰은 무제한으로 새로 발급받을 수 있어
  -- 토큰 기준이면 한 사람이 무한히 신고해 멀쩡한 글을 가릴 수 있다 (Q7).
  `reporter_ip` varchar(255) NOT NULL,

  -- AD | ABUSE | PRIVACY | ETC
  `reason` varchar(255) NOT NULL,

  `created_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  -- 같은 IP 는 같은 대상에 한 번만. 실제 방어선은 이 제약이다
  UNIQUE KEY `uk_post_report` (`target_type`,`target_id`,`reporter_ip`),
  -- 어드민 신고함은 "최근 신고를 대상별로 묶어" 본다
  KEY `idx_post_report_target` (`target_type`,`target_id`),
  KEY `idx_post_report_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
