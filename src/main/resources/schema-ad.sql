-- 광고 관리 시스템 (Ad Admin) DDL
-- ddl-auto:none 이므로 수동 실행용. docs/ad-admin-system.md 4번 항목 참고.

CREATE TABLE admin_account (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    totp_secret   VARCHAR(255) NOT NULL
);

CREATE TABLE ad_slot (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    recommended_size  VARCHAR(255),
    enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME NOT NULL
);

CREATE TABLE banner (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_id          BIGINT NOT NULL,
    image_path       VARCHAR(255) NOT NULL,
    link_url         TEXT NOT NULL,
    advertiser_name  VARCHAR(255),
    start_at         DATE NOT NULL,
    end_at           DATE NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    report_token     VARCHAR(255),
    created_at       DATETIME NOT NULL,
    CONSTRAINT fk_banner_slot FOREIGN KEY (slot_id) REFERENCES ad_slot (id)
);

CREATE TABLE banner_stat_daily (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    banner_id    BIGINT NOT NULL,
    stat_date    DATE NOT NULL,
    impressions  BIGINT NOT NULL DEFAULT 0,
    clicks       BIGINT NOT NULL DEFAULT 0,
    UNIQUE (banner_id, stat_date)
);

-- 슬롯 시드
INSERT INTO ad_slot (code, name, recommended_size, enabled, created_at)
VALUES ('PC_LEFT', 'PC 좌측', '300x600', TRUE, NOW());

INSERT INTO ad_slot (code, name, recommended_size, enabled, created_at)
VALUES ('PC_RIGHT', 'PC 우측', '300x600', TRUE, NOW());

INSERT INTO ad_slot (code, name, recommended_size, enabled, created_at)
VALUES ('MOBILE_TOP', '모바일 상단', '320x100', TRUE, NOW());
