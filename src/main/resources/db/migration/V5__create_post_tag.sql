-- 커뮤니티 태그. 설계는 docs/community-system.md Q3-1·Q3-2 (구현 5단계).
--
-- tag 마스터 테이블은 두지 않는다(문서 2항). 태그에 딸린 정보(설명·색·정렬순서)가
-- 하나도 없어서, 만들면 INSERT 전에 마스터를 조회하고 없으면 만드는 절차만 늘어난다.
-- 인기 태그는 이 테이블을 집계해 Redis 에 캐시한다.

CREATE TABLE `post_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  -- 정규화된 값이 들어간다. 공백·특수문자 제거, 영문 소문자 (#IVE 와 #ive 는 같은 태그다)
  `tag` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  -- 같은 글에 같은 태그가 두 번 들어가는 것은 앱에서도 거르지만 여기서 한 번 더 막는다
  UNIQUE KEY `uk_post_tag` (`post_id`,`tag`),
  -- 인기 태그 집계가 "최근 30일 안에 쓰인 태그를 묶어 세기"라 두 컬럼을 한 인덱스로 태운다
  KEY `idx_post_tag_tag_created` (`tag`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
