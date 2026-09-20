-- V22 가 애드센스 행에 넣은 5분·ADFIT 은 운영자가 어드민에서 정할 값이라 마이그레이션에 있으면 안 된다.
-- V22 가 넣은 값 그대로인 행만 비운다. 어드민에서 이미 바꾼 환경은 건드리지 않는다.
UPDATE `ad_network_exposure`
   SET `refill_gap_minutes` = NULL, `fallback_network` = NULL
 WHERE `network` = 'ADSENSE' AND `refill_gap_minutes` = 5 AND `fallback_network` = 'ADFIT';
