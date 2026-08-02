-- Flyway 도입 시점의 기존 스키마 (2026-07-26, 운영 DB 덤프 기준).
--
-- 이 파일은 이미 테이블이 있는 DB(prod/stage/개발자 로컬)에서는 실행되지 않는다.
-- application.yml 의 baseline-on-migrate + baseline-version:1 설정에 의해
-- "이미 적용된 것"으로만 기록되고 V2부터 실행된다.
-- 빈 DB(새 환경, 새 워크트리)에서만 실제로 실행되어 밑바탕을 만든다.
--
-- 주의: prod 에 있던 중복 인덱스 idx_pr_type_started_dur 는 여기 넣지 않았다.
--       기존 DB 에서 그것을 제거하는 일은 V2 가 맡는다.

CREATE TABLE `art` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `pixel_data` text NOT NULL,
  `width` int NOT NULL,
  `height` int NOT NULL,
  `like_count` int NOT NULL DEFAULT '0',
  `view_count` int NOT NULL DEFAULT '0',
  `comment_count` int NOT NULL DEFAULT '0',
  `client_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_art_is_public_created_at` (`created_at` DESC),
  KEY `idx_art_client_id` (`client_id`),
  KEY `idx_art_like_count` (`like_count` DESC),
  KEY `idx_art_view_count` (`view_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `art_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) NOT NULL,
  `art_id` bigint NOT NULL,
  `client_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_art_comment_art_id` (`art_id`,`created_at` DESC),
  KEY `idx_art_comment_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `art_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `art_id` bigint NOT NULL,
  `client_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_art_like_art_client` (`art_id`,`client_id`),
  KEY `idx_art_like_art_id` (`art_id`),
  KEY `idx_art_like_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `art_view` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `art_id` bigint NOT NULL,
  `client_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_art_view_art_client` (`art_id`,`client_id`),
  KEY `idx_art_view_art_id` (`art_id`),
  KEY `idx_art_view_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `captcha_result` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `client_id` bigint unsigned DEFAULT NULL,
  `elapsed_second` float NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_captcha_elapsed` (`elapsed_second`),
  KEY `idx_captcha_client_created` (`client_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `client` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token` varchar(255) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `device` varchar(255) NOT NULL,
  `referer` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `banned` tinyint(1) NOT NULL,
  `ban_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_client_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 레거시. 2025-07-31 이후 쓰기가 없고 대응하는 JPA 엔티티도 없다(Client 로 대체됨).
-- 실제 DB 상태와 맞추기 위해 baseline 에는 남긴다. 삭제는 별건으로 판단한다.
CREATE TABLE `client_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `device` text NOT NULL,
  `session_key` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `practice_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_key` varchar(255) NOT NULL,
  `nickname` varchar(255) NOT NULL,
  `type` varchar(30) NOT NULL,
  `started_at` datetime NOT NULL,
  `total_duration_ms` int NOT NULL,
  `reaction_time_ms` int NOT NULL,
  `queue_wait_ms` int NOT NULL,
  `seat_selection_ms` int NOT NULL,
  `queue_initial_rank` int NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_practice_result_client_key_type_id` (`client_key`,`type`,`id`),
  KEY `idx_practice_result_type_started_at_total_duration` (`type`,`started_at`,`total_duration_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
