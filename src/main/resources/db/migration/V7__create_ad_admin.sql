-- 광고 관리 시스템 (Ad Admin). 설계는 docs/ad-admin-system.md 4번 항목.
--
-- 이 네 테이블은 Flyway 도입 전에 schema-ad.sql 을 손으로 실행해 만들어 두었다.
-- 그래서 기존 환경(로컬·stage·prod)에는 이미 존재하고, V1 baseline 에도 안 들어가 있다.
-- 새 환경(빈 DB, 새 워크트리)에서만 실제로 만들어지면 되므로 IF NOT EXISTS 로 둔다.
-- 이 마이그레이션이 정본이 되었으니 schema-ad.sql 은 더 실행하지 않는다.
--
-- DDL 은 기존 환경의 실제 스키마를 그대로 옮겼다. "더 좋은 모양"으로 고치지 않는다 --
-- 기존 환경은 IF NOT EXISTS 로 건너뛰므로, 여기서 손대면 새 환경만 달라지는 드리프트가 된다.

CREATE TABLE IF NOT EXISTS `admin_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  -- TOTP(구글 Authenticator) 시크릿. 어드민 로그인은 비밀번호 + 6자리 코드 2단계다.
  `totp_secret` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
  -- username 에 UNIQUE 를 걸지 않은 것은 기존 스키마를 따른 것이다(계정은 수동 1건).
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 배너가 걸릴 자리. 코드는 화면이 고정으로 참조한다(PC_LEFT / PC_RIGHT / MOBILE_TOP).
CREATE TABLE IF NOT EXISTS `ad_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  -- 어드민 업로드 화면에 안내로만 띄우는 값. 검증에는 쓰지 않는다.
  `recommended_size` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot_id` bigint NOT NULL,
  -- 이미지 실물은 DB 밖(app.ad.image-dir)에 두고 경로만 갖는다.
  `image_path` varchar(255) NOT NULL,
  `link_url` text NOT NULL,
  `advertiser_name` varchar(255) DEFAULT NULL,
  -- 노출 기간. 오늘이 [start_at, end_at] 안이고 enabled 일 때만 나간다.
  `start_at` date NOT NULL,
  `end_at` date NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  -- 광고주에게 주는 성과 리포트 공개 링크의 토큰.
  `report_token` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_banner_slot` (`slot_id`),
  -- 다른 테이블과 달리 여기만 FK 가 있다. 기존 스키마를 그대로 따른 것이다.
  CONSTRAINT `fk_banner_slot` FOREIGN KEY (`slot_id`) REFERENCES `ad_slot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 노출·클릭 일별 집계. 요청마다 INSERT 하지 않고 (banner_id, stat_date) 한 행을 누적한다.
CREATE TABLE IF NOT EXISTS `banner_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `banner_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `impressions` bigint NOT NULL DEFAULT '0',
  `clicks` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  -- 이름을 붙이지 않는다. 기존 환경은 schema-ad.sql 의 무명 UNIQUE 로 만들어져
  -- MySQL 자동 이름 `banner_id` 를 갖고 있고, 여기서 이름을 주면 새 환경만 달라진다.
  UNIQUE KEY (`banner_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 슬롯 시드. 이미 있는 환경에서 중복으로 들어가지 않도록 code 로 막는다.
INSERT INTO `ad_slot` (`code`, `name`, `recommended_size`, `enabled`, `created_at`)
SELECT 'PC_LEFT', 'PC 좌측', '300x600', 1, NOW()
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_slot` WHERE `code` = 'PC_LEFT');

INSERT INTO `ad_slot` (`code`, `name`, `recommended_size`, `enabled`, `created_at`)
SELECT 'PC_RIGHT', 'PC 우측', '300x600', 1, NOW()
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_slot` WHERE `code` = 'PC_RIGHT');

INSERT INTO `ad_slot` (`code`, `name`, `recommended_size`, `enabled`, `created_at`)
SELECT 'MOBILE_TOP', '모바일 상단', '320x100', 1, NOW()
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_slot` WHERE `code` = 'MOBILE_TOP');
