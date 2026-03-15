-- 대기열 전체 리셋 스크립트 (원자적 처리: DEL ZSET + DEL Hash + DEL Sequence)
-- KEYS[1] : queue zset key (ticket:queue)
-- KEYS[2] : sequence key (ticket:queue:seq)
-- KEYS[3] : initial rank hash key (ticket:queue:info)
--
-- 반환값: 1 (성공)

-- ZSET 전체 삭제
redis.call('DEL', KEYS[1])
-- Sequence 초기화
redis.call('DEL', KEYS[2])
-- Initial Ranks Hash 전체 삭제
redis.call('DEL', KEYS[3])

return 1