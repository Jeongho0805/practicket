-- KEYS[1] : ticket (Redis Hash key)
-- ARGV    : seat1, seat2, ..., seatN, json1, json2, ..., jsonN
--
-- 반환값:
--   {1, 0} : 성공
--   {0, i} : 실패 (i번째 좌석이 이미 예약됨, 1-based)

local ticketKey = KEYS[1]
local seatCount = #ARGV / 2

-- 방어 코드: 좌석 수와 JSON 수 불일치
if seatCount * 2 ~= #ARGV then
    return {0, -1}
end

-- 좌석 중복 검사
for i = 1, seatCount do
    local seat = ARGV[i]
    if redis.call('HEXISTS', ticketKey, seat) == 1 then
        return {0, i}
    end
end

-- 2단계: 티켓 저장
for i = 1, seatCount do
    local seat = ARGV[i]
    local ticketJson = ARGV[i + seatCount]
    redis.call('HSET', ticketKey, seat, ticketJson)
end

return {1, 0}