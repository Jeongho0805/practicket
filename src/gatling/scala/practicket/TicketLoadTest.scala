package practicket

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TicketLoadTest extends Simulation {

  // HTTP 프로토콜 설정
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // 좌석 순차 증가 (A1, A2, A3, ...)
  val seatFeeder = Iterator.from(1).map(i => Map("seat" -> s"A$i"))

  // 시나리오 정의
  val ticketScenario = scenario("Ticket Reservation Load Test")
    .feed(seatFeeder) // 각 사용자에게 순차적으로 좌석 할당

    // 1. ClientKey 발급
    .exec(http("Get Client Key")
      .post("/api/client")
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("clientKey"))
    )
    .pause(100.milliseconds) // 발급 후 짧은 대기

    // 2. 대기열 진입
    .exec(http("Join Queue")
      .post("/api/order")
      .header("Authorization", "Bearer #{clientKey}")
      .check(status.is(200))
    )

    // 3. SSE 연결 및 대기열 정보 수신
    .exec(
      sse("SSE Connection").get("/api/order")
        .header("Authorization", "Bearer #{clientKey}")
        .await(600.seconds)(
          sse.checkMessage()
            .matching(_.contains("\"is_complete\":true"))  // 이 조건에 맞는 메시지만 처리
            .check(
              jsonPath("$.reservation_token").saveAs("reservationToken")
            )
        )
    )
//    .exec(
//      sse("SSE Connection").get("/api/order")
//        .header("Authorization", "Bearer #{clientKey}")
//        .await(600.seconds)(
//          sse.checkMessage("waiting-order")
//            .matching(_.contains("\"is_complete\":true"))  // 이 조건에 맞는 메시지만 처리
//            .check(
//              jsonPath("$.reservation_token").saveAs("reservationToken")
//            )
//        )
//    )
    .exec(sse("Close SSE").close)

    // 4. 티켓 예매 (reservation_token이 있을 때만)
    .doIf(session => session.contains("reservationToken")) {
      exec(http("Reserve Ticket")
        .post("/api/ticket")
        .header("Authorization", "Bearer #{clientKey}")
        .body(StringBody("""{"seats":["#{seat}"],"reservation_token":"#{reservationToken}"}""")).asJson
        .check(status.is(200))
      )
    }

  // 부하 테스트 시나리오 실행
  setUp(
    // 시나리오 1: 5분에 걸쳐 10만명 진입
    ticketScenario.inject(
      rampUsers(10).during(1.second)
    )
  ).protocols(httpProtocol)
   .maxDuration(10.minutes) // 전체 테스트 최대 10분
}
