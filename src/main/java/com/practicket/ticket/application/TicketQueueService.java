package com.practicket.ticket.application;

import com.practicket.ticket.component.TicketTokenManager;
import com.practicket.ticket.domain.TicketToken;
import com.practicket.ticket.dto.response.TicketWaitingOrderResponse;
import com.practicket.ticket.infra.redis.TicketQueueRepository;
import com.practicket.ticket.infra.redis.TicketTokenRepository;
import com.practicket.ticket.infra.sse.TicketSseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketQueueService {

    private final TicketQueueRepository queueRepository;

    private final TicketSseEmitterRepository emitterRepository;

    private final TicketTokenManager tokenManager;

    private final TicketTokenRepository tokenRepository;

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
        emitters.forEach((key, emitter) -> sendQueueInfoToClient(key, null));
    }

    public void pollQueue() {
        Long queueSize = queueRepository.size();
        if (queueSize == null || queueSize == 0L) {
            return;
        }

        // 만료된 토큰 정리
        long now = Instant.now().getEpochSecond();
        tokenRepository.cleanupExpiredTokens(now);

        // 현재 활성 토큰 개수 확인
        long activeTokens = tokenRepository.countActiveTokens();

        // 남은 작업열 공간 계산
        long availableSlots = MAX_CONCURRENT_RESERVATIONS - activeTokens;
        if (availableSlots <= 0) {
            return;
        }

        // 남은 공간만큼만 poll
        int pollCount = (int) Math.min(QUEUE_THROUGHPUT, availableSlots);
        for (int i = 0; i < pollCount; i++) {
            String clientKey = queueRepository.poll();
            if (clientKey == null) {
                continue;
            }
            TicketToken token = createToken(clientKey);
            if (token != null) {
                sendQueueInfoToClient(clientKey, token.getJwt());
            }
        }
    }

    private void sendQueueInfoToClient(String clientKey, String reservationToken) {
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
        try {
            TicketToken token = tokenManager.issue(clientKey);
            if (token != null) {
                tokenRepository.save(clientKey, token);
            }
            return token;
        } catch (Exception e) {
            log.error("create ticket token error", e);
            return null;
        }
    }

    public boolean isValidReservationToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }

        try {
            var claims = tokenManager.parseAndValidate(jwt);
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
