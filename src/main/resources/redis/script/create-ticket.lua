-- KEYS[1] : ticket (Redis Hash key)
-- KEYS[2] : token (Redis String key - ticket:token:{clientKey})
-- ARGV[1] : expectedToken (JWT string)
-- ARGV[2..N] : seat1, seat2, ..., seatN, json1, json2, ..., jsonN
--
-- 반환값:
--   {1, 0} : 성공
--   {0, -2} : 토큰 없음 또는 불일치
--   {0, -1} : 좌석 수와 JSON 수 불일치
--   {0, i} : 실패 (i번째 좌석이 이미 예약됨, 1-based)

local ticketKey = KEYS[1]
local tokenKey = KEYS[2]
local expectedToken = ARGV[1]

-- 1단계: 토큰 검증
local storedToken = redis.call('GET', tokenKey)
if not storedToken or storedToken ~= expectedToken then
    return {0, -2}
end

-- 2단계: 좌석 파라미터 파싱
local argCount = #ARGV - 1  -- 첫 번째는 토큰이므로 제외
local seatCount = argCount / 2

-- 방어 코드: 좌석 수와 JSON 수 불일치
if seatCount * 2 ~= argCount then
    return {0, -1}
end

-- 3단계: 좌석 중복 검사
for i = 1, seatCount do
    local seat = ARGV[i + 1]  -- ARGV[1]은 토큰이므로 +1
    if redis.call('HEXISTS', ticketKey, seat) == 1 then
        return {0, i}
    end
end

-- 4단계: 티켓 저장
for i = 1, seatCount do
    local seat = ARGV[i + 1]
    local ticketJson = ARGV[i + seatCount + 1]
    redis.call('HSET', ticketKey, seat, ticketJson)
end

-- 5단계: 토큰 삭제 (1회용 보장)
redis.call('DEL', tokenKey)

return {1, 0}