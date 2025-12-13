-- 티켓 생성 스크립트 (Atomic)
-- KEYS: A1, A2, A3 (좌석 번호들)
-- ARGV: ticket1_json, ticket2_json, ticket3_json (Ticket JSON 문자열들)
--
-- 반환값:
--   {1, 0}: 성공 (모든 좌석 예약 완료)
--   {0, i}: 실패 (i번째 좌석이 이미 예약됨, 1-based index)

-- 1단계: 모든 좌석이 비어있는지 체크
for i, seat in ipairs(KEYS) do
    if redis.call('HEXISTS', 'tickets', seat) == 1 then
        return {0, i}  -- i번째 좌석 이미 예약됨
    end
end

-- 2단계: 모든 티켓 저장 (Java에서 생성한 JSON)
for i, seat in ipairs(KEYS) do
    redis.call('HSET', 'tickets', seat, ARGV[i])
end

return {1, 0}  -- 성공
