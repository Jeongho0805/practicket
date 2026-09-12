-- 실전연습 중간 광고. 연습하기·전체 랭킹·내 기록 탭은 동시에 열리지 않아 슬롯은 하나만 쓴다.
--
-- 데스크톱 중앙 칸은 1280px 창에서 약 640px이므로 728px은 격자를 민다. 560x100은
-- 애드핏 320x100 모바일 소재와 한 슬롯으로 짝지을 수 있다.
--
-- fill_network 를 비워 둔다. 팔린 배너가 없으면 아무것도 안 그려진다 — 채울 네트워크를 고르는 것은
-- 실측 노출을 보고 어드민에서 정한다.
INSERT INTO `ad_slot`
  (`code`, `name`, `recommended_size`, `enabled`, `created_at`,
   `pc_width`, `pc_height`, `mobile_width`, `mobile_height`,
   `group_name`, `group_path`, `format`, `sort_order`)
SELECT 'PRACTICE_MID', '실전연습 중간', '560x100', 1, NOW(),
       560, 100, 320, 100,
       '실전연습', '/practice', '디스플레이', 10
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_slot` WHERE `code` = 'PRACTICE_MID');
