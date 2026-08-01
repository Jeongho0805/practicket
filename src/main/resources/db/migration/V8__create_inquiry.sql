-- 문의·제휴 접수. 랜딩 하단 문의 모달과 /advertise 문의 폼이 같은 테이블을 쓴다.
--
-- 접수되면 운영자에게 알림 메일이 나가지만(InquiryMailSender) 그건 best-effort 라
-- 메일이 실패해도 이 테이블에는 남는다. 그래서 이 테이블이 접수의 정본이다.
--
-- 로컬에는 이 테이블을 손으로 만들어 써왔고 Flyway 에는 안 올라가 있었다.
-- 그 스키마를 그대로 옮긴다(기존 환경은 IF NOT EXISTS 로 건너뛴다).

CREATE TABLE IF NOT EXISTS `inquiry` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  -- AD(광고·제휴) / COMPLAINT(불편·건의). 알림 메일 제목이 갈린다.
  `type` varchar(20) NOT NULL,
  -- 회신 주소. 알림 메일의 Reply-To 로 들어가 운영자가 바로 답장할 수 있다.
  `email` varchar(255) NOT NULL,
  `content` text NOT NULL,

  -- 접수자 식별용 client 토큰. 연타 방지(30초 1건)와 하루 상한(3건)의 기준이다.
  `client_token` varchar(64) DEFAULT NULL,
  -- NEW / DONE. 처리 완료 표시용.
  `status` varchar(20) NOT NULL,

  -- 다른 테이블은 datetime 이지만 여기만 datetime(6) 이다. 기존 로컬 스키마를 따른 것.
  `created_at` datetime(6) NOT NULL,

  PRIMARY KEY (`id`)
  -- 하루 상한 검사가 (client_token, created_at) 으로 세지만 인덱스는 두지 않았다.
  -- 기존 스키마에 없어서 그대로 두었고, 접수량이 늘면 별도 마이그레이션으로 추가한다.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
