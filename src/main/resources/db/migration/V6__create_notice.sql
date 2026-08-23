-- 공지·업데이트. 홈(landing) 상단 5건 + /notice 전체 목록 + 어드민 CRUD 가 함께 쓴다.
--
-- 그동안 홈의 공지 5건은 landing.html 에 하드코딩돼 있었다.
-- 공지 하나 올리려고 코드를 고쳐 재배포해야 했기에 테이블로 뺀다.
-- 다른 테이블과 마찬가지로 FK 는 걸지 않고 인덱스만 둔다.

CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  -- NOTICE(공지) / FIX(수정). 화면 배지 색이 갈린다.
  `type` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,

  -- 상단 고정. 목록에서 published 최신순보다 먼저 나온다.
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  -- 비공개면 어드민에만 보인다. 미리 써두고 나중에 여는 용도.
  `published` tinyint(1) NOT NULL DEFAULT '1',

  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,

  PRIMARY KEY (`id`),
  -- 공개 목록은 "공개된 것을 고정 먼저, 그 다음 최신순". 세 컬럼을 한 인덱스로 태운다.
  KEY `idx_notice_list` (`published`,`pinned` DESC,`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 기존 landing.html 하드코딩분을 옮겨 담는다(이관 후 화면이 비지 않도록).
INSERT INTO `notice` (`type`, `title`, `content`, `pinned`, `published`, `created_at`, `updated_at`) VALUES
  ('NOTICE', '서버 점검 안내 — 7/2(수) 새벽 2시~4시', '7월 2일(수) 새벽 2시부터 4시까지 서버 점검이 진행됩니다.\n점검 시간 동안 서비스 이용이 일시적으로 제한될 수 있습니다.', 0, 1, '2026-06-30 09:00:00', '2026-06-30 09:00:00'),
  ('FIX', '대기열 진행률이 간헐적으로 멈추던 문제 수정', '대기열 화면에서 진행률 표시가 간헐적으로 멈추던 문제를 수정했습니다.', 0, 1, '2026-06-27 09:00:00', '2026-06-27 09:00:00'),
  ('FIX', '랭킹 기록이 일부 누락되던 현상 개선', '연습 결과가 랭킹에 반영되지 않던 일부 경우를 개선했습니다.', 0, 1, '2026-06-24 09:00:00', '2026-06-24 09:00:00'),
  ('NOTICE', '야구 티켓팅 서비스 오픈 준비 안내', '야구 티켓팅 연습 서비스를 준비하고 있습니다. 오픈 일정은 확정되는 대로 공지하겠습니다.', 0, 1, '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('FIX', '모바일에서 좌석 선택이 밀리던 문제 수정', '모바일 환경에서 좌석 선택 시 터치 위치가 어긋나던 문제를 수정했습니다.', 0, 1, '2026-06-18 09:00:00', '2026-06-18 09:00:00');
