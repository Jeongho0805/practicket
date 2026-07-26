-- 커뮤니티 글. 설계는 docs/community-system.md 4항.
--
-- 컬럼은 넉넉히 잡는다(varchar(255)). 길이 검증은 앱에서 하고,
-- 컬럼 잘림으로 장애가 나지 않게 한다 (Q9).
-- 기존 테이블들과 마찬가지로 FK 제약은 걸지 않고 인덱스만 둔다.

CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,

  -- 작성 시점 스냅샷. client 를 조인하면 닉네임을 바꿨을 때
  -- 과거 글의 작성자가 전부 소급 변경된다 (Q1-1).
  `nickname` varchar(255) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `delete_password_hash` varchar(255) NOT NULL,

  -- 아래 집계·상태 컬럼은 이번 단계에서 채우지 않는다.
  -- 단계마다 ALTER 를 쌓지 않으려고 미리 만들어 둔다.
  `view_count` bigint NOT NULL DEFAULT '0',
  `like_count` bigint NOT NULL DEFAULT '0',
  `comment_count` bigint NOT NULL DEFAULT '0',
  `report_count` int NOT NULL DEFAULT '0',
  `blinded` tinyint(1) NOT NULL DEFAULT '0',
  `edited` tinyint(1) NOT NULL DEFAULT '0',

  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  -- soft delete. 행은 남긴다 (Q9 — 영구 보관, 자동 파기 배치 없음).
  `deleted_at` datetime DEFAULT NULL,

  PRIMARY KEY (`id`),
  -- 목록은 "안 지워진 글을 최신순으로". 두 컬럼을 한 인덱스로 태운다.
  KEY `idx_post_list` (`deleted_at`,`created_at` DESC),
  KEY `idx_post_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
