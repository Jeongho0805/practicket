-- 반복연습 기록. 종목마다 필요한 값이 달라 표를 나눈다.
CREATE TABLE `drill_react_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `form` varchar(20) NOT NULL,
  `reaction_ms` int NOT NULL,
  `latency_ms` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_drill_react_result_client_form` (`client_id`, `form`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `drill_captcha_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `average_ms` int NOT NULL,
  `problems` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_drill_captcha_result_client` (`client_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 사람마다 한 행. 판 기록(problems)에서 다시 만들 수 있는 요약이다.
CREATE TABLE `drill_captcha_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `letters` text NOT NULL,
  `ime_count` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drill_captcha_stat_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
