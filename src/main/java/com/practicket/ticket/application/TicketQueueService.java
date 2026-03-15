package com.practicket.ticket.application;

import com.practicket.ticket.component.TicketTokenManager;
import com.practicket.ticket.domain.TicketToken;
import com.practicket.ticket.dto.response.TicketWaitingOrderResponse;
import com.practicket.ticket.infra.redis.TicketQueueRepository;
import com.practicket.ticket.infra.redis.TicketTokenRepository;
import com.practicket.ticket.infra.sse.TicketSseEmitterRepository;
import io.jsonwebtoken.Claims;
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
        SseEmitter emitter = new SseEmitter(0L);
        emitterRepository.save(key, emitter);
        emitter.onCompletion(() -> emitterRepository.deleteByClientKey(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterRepository.deleteByClientKey(key);
        });
        emitter.onError((error) -> {
            emitter.complete();
            emitterRepository.deleteByClientKey(key);
        });
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

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
}