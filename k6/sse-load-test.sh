#!/bin/bash

COUNT=${1:-200}
CONCURRENCY=${2:-50}
SERVER="http://localhost:8080"
PID_FILE="/tmp/sse-test-pids.txt"

> "$PID_FILE"

cleanup() {
  echo "Closing..."
  while read pid; do
    kill "$pid" 2>/dev/null
  done < "$PID_FILE"
  redis-cli DEL ticketQueue > /dev/null
  rm -f "$PID_FILE"
  exit
}

trap cleanup SIGINT

echo "=== 클라이언트 생성 + 대기열 등록 + SSE 연결 ($COUNT 개, 동시 $CONCURRENCY) ==="

create_and_connect() {
  local i=$1
  # 1. 클라이언트 생성
  local RESPONSE=$(curl -s -X POST "$SERVER/api/client")
  local TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)

  if [ -z "$TOKEN" ]; then
    return
  fi

  # 2. Redis 대기열 등록
  redis-cli RPUSH ticketQueue "{\"@class\":\"com.practicket.ticket.dto.TicketQueueEventDto\",\"name\":\"$TOKEN\",\"firstWaitingOrder\":$i}" > /dev/null

  # 3. SSE 연결
  curl -s -N -H "Authorization: Bearer $TOKEN" \
    "$SERVER/api/order" > /dev/null 2>&1 &
  echo $! >> "$PID_FILE"
}

export -f create_and_connect
export SERVER PID_FILE

seq 1 $COUNT | xargs -P $CONCURRENCY -I {} bash -c 'create_and_connect "$@"' _ {}

SSE_COUNT=$(wc -l < "$PID_FILE" | tr -d ' ')
echo ""
echo "=== 완료 ==="
echo "SSE 연결 + 대기열: $SSE_COUNT 개"
echo "sendOrderByEmitter()가 매초 실행됩니다."
echo ""
echo "CPU 모니터링: top -o cpu"
echo "Press Ctrl+C to stop."

wait