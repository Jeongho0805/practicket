-- 대기열 진입 스크립트 (원자적 처리: ZREM + ZADD + HSET)
-- KEYS[1] : queue zset key (ticket:queue)
-- KEYS[2] : sequence key (ticket:queue:seq)
-- KEYS[3] : initial rank hash key (ticket:queue:info)
-- ARGV[1] : member (client-key)
-- ARGV[2] : rank field (client-key:initial-rank)

-- 이미 줄에 서 있으면 그 자리를 버린다. 화면을 떠났다 돌아온 사람은 맨 뒤로 다시 선다.
redis.call('ZREM', KEYS[1], ARGV[1])

-- 시퀀스 변수 증가
local seq = redis.call('INCR', KEYS[2])
-- ZSET 삽입
redis.call('ZADD', KEYS[1], seq, ARGV[1])
-- 삽입 직후 순번 조회
local initialRank = redis.call('ZRANK', KEYS[1], ARGV[1])
-- Hash에 초기 순번 저장 (진행도 계산 목적)
redis.call('HSET', KEYS[3], ARGV[2], initialRank)

return {1, seq, initialRank}
