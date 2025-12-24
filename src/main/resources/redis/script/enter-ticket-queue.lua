-- 대기열 진입 스크립트 (원자적 처리: ZADD + HSET)
-- KEYS[1] : queue zset key (ticket:queue)
-- KEYS[2] : sequence key (ticket:queue:seq)
-- KEYS[3] : initial rank hash key (ticket:queue:initial-ranks)
-- ARGV[1] : member (client-key)

-- 대기열에 이미 존재하는 경우
local existingScore = redis.call('ZSCORE', KEYS[1], ARGV[1])
if existingScore then
    local existingRank = redis.call('ZRANK', KEYS[1], ARGV[1])
    return {0, tonumber(existingScore), existingRank}
end

-- 시퀀스 변수 증가
local seq = redis.call('INCR', KEYS[2])
-- ZSET 삽입
redis.call('ZADD', KEYS[1], seq, ARGV[1])
-- 삽입 직후 순번 조회
local initialRank = redis.call('ZRANK', KEYS[1], ARGV[1])
-- Hash에 초기 순번 저장 (진행도 계산 목적)
redis.call('HSET', KEYS[3], ARGV[1], initialRank)

return {1, seq, initialRank}
