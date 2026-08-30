-- blog-post-migration.sql 실행 뒤 이관이 온전한지 판정한다.
--
-- 이 검증을 3단계(화면 전환) 배포의 관문으로 쓴다. 1차 배포 시점의 화면은
-- 여전히 정적 파일을 읽기 때문에, 이관이 깨져도 사용자 트래픽으로는 드러나지 않는다.
-- 사람 눈이 아니라 이 쿼리가 통과해야 넘어간다.
--
-- 실행: mysql -u<user> -p practicket -t < scripts/blog-post-verify.sql
-- 결과의 result 열이 하나라도 FAIL 이면 진행하지 않는다.

SELECT '1. 행 개수' AS check_name,
       '15' AS expected,
       CAST(COUNT(*) AS CHAR) AS actual,
       IF(COUNT(*) = 15, 'PASS', 'FAIL') AS result
FROM `blog_post`

UNION ALL
-- 색인된 /blog/1 ~ /blog/15 가 전부 살아 있어야 한다
SELECT '2. id 1~15 존재', '15',
       CAST(COUNT(*) AS CHAR),
       IF(COUNT(*) = 15, 'PASS', 'FAIL')
FROM `blog_post` WHERE `id` BETWEEN 1 AND 15

UNION ALL
-- 가장 짧은 글이 2,594자다. 이보다 크게 짧으면 본문이 잘린 것이다
SELECT '3. 본문 2000자 이상', '0',
       CAST(COUNT(*) AS CHAR),
       IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM `blog_post` WHERE CHAR_LENGTH(`content`) < 2000

UNION ALL
SELECT '4. 제목·부제·썸네일 빈 값', '0',
       CAST(COUNT(*) AS CHAR),
       IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM `blog_post`
WHERE `title` IS NULL OR `title` = ''
   OR `subtitle` IS NULL OR `subtitle` = ''
   OR `thumbnail_image_path` IS NULL OR `thumbnail_image_path` = ''

UNION ALL
-- 미발행이거나 발행 시각이 비면 목록·sitemap 에서 빠진다
SELECT '5. 공개 상태·발행 시각', '0',
       CAST(COUNT(*) AS CHAR),
       IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM `blog_post`
WHERE `status` <> 'PUBLISHED' OR `published_at` IS NULL

UNION ALL
-- 본문 이미지는 전부 /blog-images/ 로 옮겼다. /image/ 가 남아 있으면
-- 3단계에서 static 사본을 지우는 순간 그 이미지가 깨진다
SELECT '6. 본문의 /image/ 잔존', '0',
       CAST(COUNT(*) AS CHAR),
       IF(COUNT(*) = 0, 'PASS', 'FAIL')
FROM `blog_post` WHERE `content` LIKE '%/image/%';
