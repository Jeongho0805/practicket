-- 토큰 저장 스크립트 (원자적 처리: ZADD + HSET)
-- KEYS[1] : active tokens zset key (ticket:active-tokens)
-- KEYS[2] : queue info hash key (ticket:queue:info)
-- ARGV[1] : jti (token id)
-- ARGV[2] : expiration timestamp
-- ARGV[3] : token field (client-key:token)
-- ARGV[4] : jwt (token string)

-- ZSet에 토큰 저장 (작업열 관리용)
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
-- Hash에 JWT 전문 저장 (재연결 시 조회용)
redis.call('HSET', KEYS[2], ARGV[3], ARGV[4])

return 1
