# Gatling 부하 테스트 가이드

10만명 동시 접속 티켓 예매 시스템 부하 테스트 가이드입니다.

## 사전 준비

### 1. 테스트용 코드 수정 완료 확인

`TicketScheduler.java`에서 다음 4개 메서드가 주석 처리되어 있는지 확인:
- `addVirtualUserOnQueue()` - 가상 유저 추가 (주석 처리됨 ✅)
- `issueTicketForVirtualUser()` - 가상 유저 티켓 예매 (주석 처리됨 ✅)
- `adjustStartTime()` - 시작 시간 조정 (주석 처리됨 ✅)
- `clearAllRecord()` - 데이터 초기화 (주석 처리됨 ✅)

### 2. 시작 시간 검증 우회 (선택적)

`POST /api/order` 호출 시 `validateStartTime()`이 실행됩니다.
만약 "티켓팅 시간이 아닙니다" 에러가 발생하면 다음 중 하나를 선택:

**옵션 A: TicketService.validateStartTime() 임시 주석 처리**
```java
// src/main/java/com/practicket/ticket/application/TicketService.java
public void validateStartTime() {
    // 부하 테스트를 위해 임시 주석 처리
    // if (!ticketTimer.isValidStartTime()) {
    //     throw new TicketException(ErrorCode.TICKETING_TIME_IS_NOT_ALLOWED);
    // }
}
```

**옵션 B: 시작 시간을 현재보다 이전으로 설정**
- Redis에 저장된 티켓 시작 시간을 조정하거나
- TicketTimer 로직 확인 후 수동 조정

## Gatling 테스트 실행

### 1. Gradle 의존성 다운로드

```bash
./gradlew build -x test
```

### 2. 애플리케이션 실행

```bash
# 로컬 환경
./gradlew bootRun

# 또는 특정 프로파일
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### 3. Gatling 테스트 실행

**3-1. 10만명 부하 테스트 (5분간 램프업)**

```bash
./gradlew gatlingRun --simulation=practicket.TicketLoadTest

# 또는 (시뮬레이션이 하나만 있으면)
./gradlew gatlingRun
```

- 사용자: 100,000명
- 진입 시간: 5분에 걸쳐 균등 분산
- 최대 실행 시간: 10분
- 시나리오:
  1. ClientKey 발급
  2. 대기열 진입
  3. SSE로 대기 (최대 3분)
  4. 작업열 토큰 받으면 티켓 예매

### 4. 테스트 결과 확인

테스트 완료 후 HTML 리포트가 자동 생성됩니다:

```
build/reports/gatling/ticketloadtest-<타임스탬프>/index.html
```

브라우저로 열어서 확인:
- 초당 요청 수 (RPS)
- 응답 시간 분포
- 성공/실패율
- 95/99 백분위 응답 시간

## 모니터링 포인트

### 애플리케이션 로그 확인

```bash
# 실시간 로그 확인
tail -f logs/spring.log

# 또는 콘솔 출력 확인
```

### Redis 모니터링

```bash
# Redis CLI 접속
redis-cli

# 대기열 크기 확인
ZCARD waiting-order

# 작업열 토큰 수 확인
KEYS ticket:token:*

# 메모리 사용량 확인
INFO memory
```

### 시스템 리소스 확인

```bash
# CPU/메모리 사용량
top

# 네트워크 연결 수
netstat -an | grep 8080 | wc -l
```

## 성능 지표 확인 항목

1. **처리량 (Throughput)**
   - 초당 대기열 진입 수
   - 초당 티켓 예매 완료 수

2. **대기 시간**
   - 대기열 진입 → 작업열 토큰 발급까지 시간
   - 작업열 토큰 발급 → 티켓 예매 완료까지 시간

3. **동시 처리 능력**
   - MAX_CONCURRENT_RESERVATIONS = 5000 잘 동작하는지
   - QUEUE_THROUGHPUT = 10 (초당 10명씩 작업열 진입)

4. **에러율**
   - HTTP 5xx 에러 비율
   - 타임아웃 비율
   - 토큰 검증 실패 비율

## 예상 동작

### 정상 시나리오

1. **초기 (0-30초)**
   - 사용자들이 빠르게 대기열에 진입
   - 대기열 크기 급증

2. **중반 (30초-3분)**
   - pollQueue가 매초 실행되며 10명씩 작업열로 이동
   - 작업열 슬롯이 5000개이므로, 초기 500초(약 8분)간 빠르게 처리
   - 5000명이 작업열에 있으면 더 이상 진입 중지, 티켓 예매 완료되어야 슬롯 확보

3. **후반 (3분-10분)**
   - 대기열에서 순차적으로 처리
   - SSE 연결 유지하며 대기

### 병목 예상 지점

1. **Redis 부하**
   - ZSET 연산 (대기열)
   - 토큰 저장/조회

2. **SSE 연결 수**
   - 10만개 SSE 연결 동시 유지
   - 메모리 소모 주의

3. **ThreadPool 포화**
   - ticketTaskExecutor 설정 확인 필요

## 테스트 후 정리

### 1. 코드 원복

주석 처리했던 스케줄러 메서드 복원:
```bash
# TicketScheduler.java 주석 해제
# TicketService.validateStartTime() 주석 해제 (했다면)
```

### 2. 데이터 정리

```bash
# Redis 데이터 삭제
redis-cli FLUSHDB

# MySQL 티켓 데이터 삭제 (필요 시)
```

### 3. 애플리케이션 재시작

```bash
# 프로세스 종료 후 재시작
./gradlew bootRun
```

## 트러블슈팅

### "티켓팅 시간이 아닙니다" 에러

→ `validateStartTime()` 주석 처리 (위 "사전 준비 2" 참고)

### SSE 연결 타임아웃

→ `TicketLoadTest.scala`의 `.await(180.seconds)` 시간 늘리기

### OutOfMemoryError

→ JVM 힙 메모리 증가:
```bash
export JAVA_OPTS="-Xmx4g -Xms2g"
./gradlew bootRun
```

### Redis Connection Pool Exhausted

→ `application.yml`에서 Redis 연결 풀 설정 확인:
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 100
          max-idle: 50
```

### 테스트 중간에 중단하고 싶을 때

```bash
# Ctrl + C 로 Gatling 중단
# 애플리케이션도 중단하고 데이터 정리 후 재시작
```

## 성능 개선 팁

테스트 결과가 나쁘면 다음을 고려:

1. **Redis 최적화**
   - Pipeline 사용
   - Lua 스크립트로 다중 명령 원자화

2. **ThreadPool 튜닝**
   - `ticketTaskExecutor` 크기 조정
   - Core/Max Pool Size 증가

3. **SSE 최적화**
   - Broadcast 주기 조정 (현재 1초마다)
   - 비활성 연결 정리 로직

4. **Database 인덱스**
   - 티켓 조회 쿼리 최적화
   - Connection Pool 크기 조정

---

**중요**: 테스트 완료 후 반드시 주석 처리했던 코드를 원복하세요!