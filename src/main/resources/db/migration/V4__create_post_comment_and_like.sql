-- 커뮤니티 댓글·추천. 설계는 docs/community-system.md 4항 (구현 6단계).
--
-- post 의 like_count·comment_count·view_count 는 V3 에서 미리 만들어 두었으므로
-- 여기서 ALTER 하지 않는다.

-- 댓글은 1단만이다 (Q4-4). parent_id 를 "혹시 몰라" 미리 만들지 않는다 —
-- 대댓글은 미확정 기능이고, 안 쓸 컬럼은 나중에 "이건 왜 있지"가 된다.
-- 뒤집는 비용은 컬럼 추가 + 조회 쿼리 교체뿐이다.
--
-- delete_password_hash 도 두지 않는다. 댓글마다 네 자리를 받으면 마찰이 커서
-- 댓글이 안 달린다. 삭제는 토큰이 작성자와 같을 때만 되고,
-- 기기가 바뀐 경우는 신고 → 어드민 모더레이션(9단계)이 받는다.
CREATE TABLE `post_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `client_id` bigint NOT NULL,
  `content` text NOT NULL,

  -- 글과 같은 이유로 작성 시점 스냅샷이다 (Q1-1).
  `nickname` varchar(255) NOT NULL,
  `ip` varchar(255) NOT NULL,

  `report_count` int NOT NULL DEFAULT '0',
  `blinded` tinyint(1) NOT NULL DEFAULT '0',

  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime DEFAULT NULL,

  PRIMARY KEY (`id`),
  -- 댓글은 "한 글의 안 지워진 댓글을 작성순으로". 세 컬럼을 한 인덱스로 태운다.
  KEY `idx_post_comment_list` (`post_id`,`deleted_at`,`created_at`),
  KEY `idx_post_comment_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 추천. ArtLike 와 같은 패턴이다.
-- 유니크 제약이 실제 중복 방지선이다 — 동시에 두 번 눌러도 DB 가 막는다.
-- 취소하면 행을 지운다(soft delete 아님). 누가 언제 추천했다 취소했는지는 남길 이유가 없다.
CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `client_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_like` (`post_id`,`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
