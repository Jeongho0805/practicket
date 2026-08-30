-- 블로그 글. /blog 목록 + /blog/{id} 상세 + sitemap + 어드민 CRUD 가 함께 쓴다.
--
-- 그동안 글 15개는 templates/blog/1~15.html 로 파일마다 하드코딩돼 있었다.
-- 글 하나 올리려고 HTML 을 만들고 재배포해야 했기에 테이블로 뺀다.
-- 파일마다 손으로 적다 보니 제목이 h1 · <title> · 목록 카드로, 설명이 부제 ·
-- meta description 으로 갈라져 서로 어긋나 있었다. 여기서는 title 과 subtitle
-- 하나씩만 정본으로 두고 나머지는 화면에서 파생시킨다.
--
-- 기존 15건은 색인된 URL 을 지켜야 해서 id 를 1~15 로 고정해 넣는다.
-- 그 INSERT 는 이 파일에 없다(ADR 0002). Flyway 밖의 이관 SQL 로 따로 실행한다.

CREATE TABLE `blog_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  -- 정본 제목. h1 · 목록 카드 · <title> 이 모두 이 값에서 나온다.
  -- <title> 은 화면에서 "제목 - 프랙티켓" 으로 만든다
  `title` varchar(255) NOT NULL,
  -- 상세 화면 제목 아래 한 줄. 목록 카드 설명과 검색결과 요약도 이 값을 쓴다.
  -- 파일에는 부제와 meta description 이 따로 있었는데, 같은 글을 소개하는
  -- 문장이 두 벌로 갈라져 어긋나 있어 하나로 합쳤다
  `subtitle` varchar(500) DEFAULT NULL,

  -- 목록 카드 썸네일. 상세 화면에는 그리지 않는다
  `thumbnail_image_path` varchar(255) DEFAULT NULL,
  `content` longtext NOT NULL,

  -- PUBLISHED | UNPUBLISHED. notice 와 달리 boolean 이 아닌 이유는,
  -- 나중에 검토·반려 상태가 붙을 때 ALTER 없이 값만 늘리기 위해서다
  `status` varchar(20) NOT NULL DEFAULT 'PUBLISHED',
  `view_count` bigint NOT NULL DEFAULT '0',

  -- 미발행 글에는 발행 시각이 없다
  `published_at` datetime DEFAULT NULL,

  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  -- 공개 목록은 "공개된 것을 최신순". id 까지 넣는 이유는 발행 시각이 겹치기 때문이다.
  -- 기존 15건은 파일 생성일이 3개 시각에 뭉쳐 있어, id 가 없으면 순서가 매번 달라진다
  KEY `idx_blog_post_list` (`status`,`published_at` DESC,`id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
