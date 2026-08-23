-- 기간제 밴. docs/community-system.md Q8.
--
-- 지금까지 `client.banned` 는 boolean 이라 영구정지밖에 못 표현했다.
-- `banned_until` 을 추가해 도배 1일 / 반복 7일 / 악질 영구(banned=true, banned_until=NULL) 을 구분한다.
--
-- 별도 해제 배치는 두지 않는다. banned_until 이 지났는지는 글/댓글을 저장하는 시점에
-- 그때그때 판단한다(Client.isBanned) — banned 컬럼 값 자체는 건드리지 않는다.
ALTER TABLE `client`
  ADD COLUMN `banned_until` datetime NULL AFTER `ban_reason`;
