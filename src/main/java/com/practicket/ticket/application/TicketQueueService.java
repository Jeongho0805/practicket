package com.practicket.ticket.application;

import com.practicket.ticket.component.TicketTokenManager;
import com.practicket.ticket.domain.TicketToken;
import com.practicket.ticket.dto.response.TicketWaitingOrderResponse;
import com.practicket.ticket.infra.redis.TicketQueueRepository;
import com.practicket.ticket.infra.redis.TicketTokenRepository;
import com.practicket.ticket.infra.sse.TicketSseEmitterRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketQueueService {

    private final TicketQueueRepository queueRepository;
    private final TicketSseEmitterRepository emitterRepository;
    private final TicketTokenManager tokenManager;
    private final TicketTokenRepository tokenRepository;
    private final ThreadPoolTaskExecutor ticketTaskExecutor;

    /** 무한(0)이면 얼어붙은 연결을 아무도 못 걷어낸다. 만료돼도 브라우저가 곧바로 다시 붙는다. */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private static final int QUEUE_THROUGHPUT = 10;
    private static final int MAX_CONCURRENT_RESERVATIONS = 5000;
    private static final String WAITING_QUEUE_NAME = "waiting-order";

    public void enterQueue(String key) {
        queueRepository.enterQueue(key);
    }

    public void initData() {
        queueRepository.deleteAll();
    }

    public SseEmitter saveEmitter(String key) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitterRepository.save(key, emitter);
        // 콜백 안에서 complete() 를 부르면 안 된다. 컨테이너가 비동기요청 락을 쥔 채 부르는 자리라,
        // 거기서 emitter 모니터를 요구하면 방송 스레드와 락 순서가 엇갈려 서로 영구 대기한다.
        emitter.onCompletion(() -> emitterRepository.deleteByClientKey(key));
        emitter.onTimeout(() -> emitterRepository.deleteByClientKey(key));
        emitter.onError((error) -> emitterRepository.deleteByClientKey(key));
        return emitter;
    }

    public void broadcastQueueInfo() {
        Map<String, SseEmitter> emitters = emitterRepository.findAll();
        if (emitters.isEmpty()) {
            return;
        }
        emitters.forEach((key, emitter) -> sendQueueInfoToClient(key));
    }

    public void pollQueue() {
        Long queueSize = queueRepository.size();
        if (queueSize == null || queueSize == 0L) {
            return;
        }
        // 작업열 여유 슬롯 계산
        long now = Instant.now().getEpochSecond();
        tokenRepository.cleanupExpiredTokens(now);
        long activeTokens = tokenRepository.countActiveTokens();
        long availableSlots = MAX_CONCURRENT_RESERVATIONS - activeTokens;
        if (availableSlots <= 0) {
            return;
        }
        // 대기열 poll - 병렬 처리
        int pollCount = (int) Math.min(QUEUE_THROUGHPUT, availableSlots);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < pollCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                TypedTuple<String> poppedElement = queueRepository.pollWithScore();
                if (poppedElement == null) {
                    return;
                }
                String clientKey = poppedElement.getValue();
                Double score = poppedElement.getScore();
                if (score == null) {
                    return;
                }
                try {
                    createToken(clientKey);
                } catch (Exception e) {
                    queueRepository.requeue(clientKey, score);
                    log.error("Token creation failed for clientKey: {}, requeued", clientKey, e);
                }
            }, ticketTaskExecutor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private void sendQueueInfoToClient(String clientKey) {
        SseEmitter emitter = emitterRepository.get(clientKey);
        if (emitter == null) {
            return;
        }
        Long initialRank = queueRepository.getInitialRank(clientKey);
        if (initialRank == null) {
            log.error("initial rank is null for clientKey: {}", clientKey);
            emitterRepository.deleteByClientKey(clientKey);
            return;
        }
        String reservationToken = queueRepository.getToken(clientKey);
        Long currentRank = queueRepository.getCurrentRank(clientKey);
        TicketWaitingOrderResponse response = new TicketWaitingOrderResponse(
                currentRank,
                initialRank,
                reservationToken
        );
        try {
            emitter.send(SseEmitter.event()
                    .name(WAITING_QUEUE_NAME)
                    .data(response));
            if (response.isComplete()) {
                emitterRepository.deleteByClientKey(clientKey);
            }
        } catch (IOException e) {
            emitterRepository.deleteByClientKey(clientKey);
        }
    }

    public TicketToken createToken(String clientKey) {
        TicketToken token = tokenManager.issue(clientKey);
        tokenRepository.saveWithQueueInfo(clientKey, token);
        return token;
    }

    public boolean isValidReservationToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }
        try {
            Claims claims = tokenManager.parseAndValidate(jwt);
            String jti = claims.getId();
            long now = Instant.now().getEpochSecond();

            Long expirationTime = tokenRepository.getExpirationTime(jti);
            if (expirationTime == null) {
                return false;
            }

            return expirationTime >= now;

        } catch (JwtException e) {
            log.warn("유효하지 않은 예약 토큰 요청: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("예약 토큰 검증 중 예상치 못한 오류", e);
            return false;
        }
    }
}