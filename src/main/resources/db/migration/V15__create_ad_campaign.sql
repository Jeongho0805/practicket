-- 광고 계약(캠페인) 층 도입.
--
-- 지금까지 광고주는 banner.advertiser_name 문자열 한 칸이었고 계약이라는 개념이 없었다.
-- "이 거래처와 얼마짜리 계약을 했고 어느 자리를 쓰는가" 를 물을 데가 없어 배너를 일일이 세야 했다.
-- 광고주 → 계약 → 배너 3층을 만들고 기존 배너를 계약 밑으로 옮긴다.
--
-- 이 파일은 더하기만 한다. advertiser_name · report_token · recommended_size · enabled 는
-- 쓰지 않게 된 뒤에도 그대로 둔다. k3s 롤링 배포는 파드를 하나씩 교체하는데, 그동안 구버전
-- 파드가 아직 트래픽을 받는다. 광고 조회가 layout/default 에 있어 그 컬럼이 사라지면
-- 광고만 비는 게 아니라 전 페이지가 500 이 된다. 삭제는 모든 파드가 새 코드로 바뀐
-- 다음 릴리스에서 한다.
--
-- 외래키와 CHECK 는 쓰지 않는다. 레포 전체가 그렇다(테이블 21개 중 FK 1개, CHECK 0개).
-- 값 검증은 애플리케이션이 한다.

-- 거래처. 지금까지 배너에 이름만 적혀 있어 연락처도 계약 이력도 둘 데가 없었다.
CREATE TABLE IF NOT EXISTS `advertiser` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  -- 어드민에서 목록으로 고르는 이름. 같은 거래처가 두 번 생기면 합칠 방법이 없어 UNIQUE 로 막는다
  `name` varchar(255) NOT NULL,
  `company` varchar(255) DEFAULT NULL,
  `manager` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `biz_no` varchar(255) DEFAULT NULL,
  `memo` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_advertiser_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 계약 한 건. 자리 하나짜리 거래도 예외 없이 계약 1건 + 배너 1개로 감싼다.
CREATE TABLE IF NOT EXISTS `ad_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `advertiser_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `start_at` date NOT NULL,
  `end_at` date NOT NULL,
  -- 원 단위. 진행·예정·종료 상태는 저장하지 않고 기간과 오늘을 비교해 계산한다.
  -- 저장하면 날짜와 어긋나는 순간이 반드시 온다
  `amount` bigint NOT NULL DEFAULT '0',
  -- 계약 기본 착지 주소. 배너가 link_url 을 비우면 이 값을 쓴다
  `link_url` text,
  -- 광고주에게 주는 성과 리포트 공개 링크의 토큰. 배너마다 따로이던 것을 계약 단위로 올렸다.
  -- 자리를 셋 산 광고주가 링크를 셋 받던 것이 하나가 되고, 수치는 그 안에서 합산된다
  `report_token` varchar(64) NOT NULL,
  `memo` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ad_campaign_report_token` (`report_token`),
  KEY `idx_ad_campaign_advertiser` (`advertiser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 팔리지 않은 자리를 채울 네트워크 광고단위.
CREATE TABLE IF NOT EXISTS `ad_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  -- ADSENSE | ADFIT | COUPANG
  `network` varchar(255) NOT NULL,
  -- 네트워크가 발급한 단위 식별자. 계정 단위 값(쿠팡 trackingCode, 애드센스 client)은
  -- 사이트에 하나뿐이고 바뀌지 않아 여기 두지 않는다 — application.yml 이 갖는다
  `unit_id` varchar(255) NOT NULL,
  -- 둘 다 NULL 이면 반응형이라 규격을 안 가린다(애드센스·쿠팡).
  -- 애드핏은 코드에 숫자를 박는 방식이라 값이 있고, 자리보다 작거나 같을 때만 들어간다
  `width` smallint unsigned DEFAULT NULL,
  `height` smallint unsigned DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ad_unit_network_unit` (`network`,`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 자리는 지금까지 기기별로 갈려 있어(PC_LEFT / MOBILE_TOP) 규격 한 칸으로 충분했다.
-- 앞으로는 한 자리가 화면 폭에 따라 두 규격으로 나간다(글 목록 사이 = 데스크톱 728x90 / 모바일 300x250).
-- 그리고 이 값은 어드민 안내문이 아니라 미판매 자리에 넣을 광고단위를 고르는 기준이 되므로,
-- 조회할 때마다 문자열을 쪼개지 않도록 숫자로 나눈다. NULL 은 "그 기기에서는 안 나감" 이다.
ALTER TABLE `ad_slot`
  ADD COLUMN `pc_width` smallint unsigned DEFAULT NULL,
  ADD COLUMN `pc_height` smallint unsigned DEFAULT NULL,
  ADD COLUMN `mobile_width` smallint unsigned DEFAULT NULL,
  ADD COLUMN `mobile_height` smallint unsigned DEFAULT NULL,
  -- 어드민 목록의 묶음. 지금은 코드에 박혀 있다
  ADD COLUMN `group_name` varchar(255) DEFAULT NULL,
  ADD COLUMN `group_path` varchar(255) DEFAULT NULL,
  -- 디스플레이 | 인피드 | 인아티클 | 멀티플렉스
  ADD COLUMN `format` varchar(255) DEFAULT NULL,
  ADD COLUMN `sort_order` int NOT NULL DEFAULT '0',
  -- 팔리지 않았을 때 채울 네트워크. NULL 이면 비워둔다 = 그 자리를 아예 그리지 않는다.
  -- 자리마다 켜고 끄는 스위치를 따로 두지 않는 이유가 이것이다
  ADD COLUMN `fill_network` varchar(255) DEFAULT NULL,
  -- 사람이 직접 고른 광고단위. NULL 이면 규격이 맞는 것을 자동으로 고른다
  ADD COLUMN `pc_ad_unit_id` bigint DEFAULT NULL,
  ADD COLUMN `mobile_ad_unit_id` bigint DEFAULT NULL,
  -- code 는 화면이 고정으로 참조하는 값인데 지금까지 중복을 막는 것이 없었다.
  -- 같은 코드가 두 행이면 배너가 갈려 붙어 어느 쪽이 나갈지 알 수 없다
  ADD UNIQUE KEY `uk_ad_slot_code` (`code`);

-- 레포에 남은 단 하나의 외래키를 없앤다. 나머지 20개 테이블은 관계를 컬럼으로만 두고
-- 무결성은 애플리케이션이 트랜잭션 안에서 지킨다. 여기만 예외였던 것을 맞춘다.
-- 운영·스테이지 모두 이름이 fk_banner_slot 으로 같은 것을 확인했다.
-- slot_id 인덱스는 KEY 로 따로 선언돼 있어 제약만 사라지고 조회 성능은 그대로다.
ALTER TABLE `banner` DROP FOREIGN KEY `fk_banner_slot`;

-- 배너를 계약 밑으로 옮긴다. 기간과 링크를 비울 수 있게 되고, 비우면 계약 값을 따른다.
-- 계약 기간 중 모바일만 2주 먼저 내리는 식으로 덮어쓸 때만 채운다.
--
-- 그림은 기기별로 나눈다. 자리 규격이 데스크톱·모바일 둘로 갈리므로 한 장으로는 안 된다.
-- image_path 의 NOT NULL 을 푸는 이유는 두 가지다. 하나는 새 칸만 채우는 배너를 저장할 수
-- 있어야 하는 것이고, 다른 하나는 자리를 고른 뒤 소재를 나중에 붙이는 중간 상태를 허용하는 것이다.
-- 그림이 하나도 없는 배너는 조회에서 빼서 내보내지 않는다(그 자리는 네트워크가 채운다).
-- 조건을 푸는 변경이라 롤링 배포 중 구버전 파드가 값을 채워 넣어도 그대로 통과한다.
ALTER TABLE `banner`
  ADD COLUMN `campaign_id` bigint DEFAULT NULL,
  ADD COLUMN `pc_image_path` varchar(255) DEFAULT NULL,
  ADD COLUMN `mobile_image_path` varchar(255) DEFAULT NULL,
  MODIFY COLUMN `image_path` varchar(255) DEFAULT NULL,
  MODIFY COLUMN `start_at` date DEFAULT NULL,
  MODIFY COLUMN `end_at` date DEFAULT NULL,
  MODIFY COLUMN `link_url` text,
  ADD KEY `idx_banner_campaign` (`campaign_id`);

-- 기존 자리 3개에 새 값을 채운다. 자리가 없으면 배너를 붙일 곳이 없으므로 기준 데이터다.
UPDATE `ad_slot` SET
  `pc_width` = 300, `pc_height` = 600,
  `group_name` = '전 페이지 공통', `group_path` = 'GLOBAL',
  `format` = '디스플레이', `sort_order` = 1, `fill_network` = 'COUPANG'
WHERE `code` = 'PC_LEFT';

UPDATE `ad_slot` SET
  `pc_width` = 300, `pc_height` = 600,
  `group_name` = '전 페이지 공통', `group_path` = 'GLOBAL',
  `format` = '디스플레이', `sort_order` = 2, `fill_network` = 'ADSENSE'
WHERE `code` = 'PC_RIGHT';

UPDATE `ad_slot` SET
  `mobile_width` = 320, `mobile_height` = 100,
  `group_name` = '전 페이지 공통', `group_path` = 'GLOBAL',
  `format` = '디스플레이', `sort_order` = 3, `fill_network` = 'COUPANG'
WHERE `code` = 'MOBILE_TOP';

-- 지금 템플릿에 하드코딩돼 있는 단위 둘. 위 fill_network 가 가리킬 대상이라 함께 넣는다.
INSERT IGNORE INTO `ad_unit` (`network`, `unit_id`, `name`, `created_at`)
VALUES ('COUPANG', '943782', '쿠팡 파트너스 기본', NOW()),
       ('ADSENSE', '9697904962', '애드센스 기본', NOW());

-- ── 기존 배너 이관 ─────────────────────────────────────────────
-- 광고주 이름 문자열만 있던 배너를 광고주·계약 밑으로 옮긴다.
-- prod(욱재 2건) 와 stage(호야테스트 1건 · 욱재 2건) 를 확인했고, 두 환경 모두
-- 같은 광고주의 배너는 기간이 완전히 같아 아래 MIN/MAX 로 묶어도 기간이 뒤틀리지 않는다.
-- 계약 이름은 SQL 이 알 수 없으므로 광고주 이름을 그대로 넣는다. 어드민에서 고친다.

-- 이름을 trim 해서 넣는다. 애플리케이션이 이미 그렇게 다루고 있어(AdminAdStatService) 맞춘다.
INSERT IGNORE INTO `advertiser` (`name`, `created_at`)
SELECT DISTINCT TRIM(b.`advertiser_name`), NOW()
  FROM `banner` b
 WHERE b.`advertiser_name` IS NOT NULL
   AND TRIM(b.`advertiser_name`) <> '';

-- 광고주마다 계약 하나. campaign_id 가 아직 빈 배너만 보므로 다시 실행해도 늘지 않는다.
INSERT INTO `ad_campaign`
  (`advertiser_id`, `name`, `start_at`, `end_at`, `amount`, `report_token`, `created_at`)
-- UUID() 는 시간·장비 주소 기반이라 같은 시점에 만든 값끼리 대부분이 겹친다.
-- 리포트 주소는 로그인 없이 열리므로 하나를 알면 남의 것을 맞힐 수 있어 난수를 쓴다.
SELECT a.`id`, a.`name`, MIN(b.`start_at`), MAX(b.`end_at`), 0,
       HEX(RANDOM_BYTES(16)), NOW()
  FROM `banner` b
  JOIN `advertiser` a ON a.`name` = TRIM(b.`advertiser_name`)
 WHERE b.`campaign_id` IS NULL
 GROUP BY a.`id`, a.`name`;

UPDATE `banner` b
  JOIN `advertiser` a ON a.`name` = TRIM(b.`advertiser_name`)
  JOIN `ad_campaign` c ON c.`advertiser_id` = a.`id`
   SET b.`campaign_id` = c.`id`
 WHERE b.`campaign_id` IS NULL;

-- 한 장뿐이던 그림을 기기별 칸으로 옮긴다. 자리 규격이 있는 쪽에만 넣는다.
-- 지금 배너 3건은 데스크톱 전용 자리(PC_LEFT·PC_RIGHT) 또는 모바일 전용 자리(MOBILE_TOP)라
-- 각각 한쪽만 채워진다. 양쪽 다 나가는 자리라면 같은 그림을 둘 다 넣는다 --
-- 규격이 달라 따로 필요하면 어드민에서 한쪽만 바꿔 올리면 된다.
-- 위 ad_slot UPDATE 로 규격이 채워진 뒤라야 하므로 순서를 지킨다.
UPDATE `banner` b
  JOIN `ad_slot` s ON s.`id` = b.`slot_id`
   SET b.`pc_image_path`     = IF(s.`pc_width` IS NOT NULL, b.`image_path`, NULL),
       b.`mobile_image_path` = IF(s.`mobile_width` IS NOT NULL, b.`image_path`, NULL)
 WHERE b.`image_path` IS NOT NULL
   AND b.`pc_image_path` IS NULL
   AND b.`mobile_image_path` IS NULL;
