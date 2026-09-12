-- 실전연습 중간 광고를 목록 행 높이(52px)의 네이티브 행으로 낮춘다. PC 560x52 · 모바일 320x52.
-- 모바일은 애드핏 320x50 이 들어가고, PC 는 애드센스·쿠팡 반응형이 채운다.
-- 직접 지정한 광고단위는 새 규격에 안 맞으므로 자동 선택으로 되돌린다.
UPDATE `ad_slot`
   SET `recommended_size` = '560x52',
       `pc_width` = 560, `pc_height` = 52,
       `mobile_width` = 320, `mobile_height` = 52,
       `pc_ad_unit_id` = NULL, `mobile_ad_unit_id` = NULL
 WHERE `code` = 'PRACTICE_MID';
