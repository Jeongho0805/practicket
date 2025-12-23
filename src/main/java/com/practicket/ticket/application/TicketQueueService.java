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
        emitters.forEach((key, emitter) -> {
            Long currentRank = queueRepository.getCurrentRank(key);
            Long initialRank = queueRepository.getInitialRank(key);
            // 초기 순위가 없으면 비정상 데이터. emitter 삭제 처리
            if (initialRank == null) {
                log.error("initial rank is null");
                emitterRepository.deleteByClientKey(key);
                return;
            }
            String reservationToken = tokenRepository.get(key);
            TicketWaitingOrderResponse ticketWaitingOrder = new TicketWaitingOrderResponse(currentRank, initialRank, reservationToken);
            try {
                emitter.send(SseEmitter.event()
                        .name(WAITING_QUEUE_NAME)
                        .data(ticketWaitingOrder));
                if (ticketWaitingOrder.isComplete()) {
                    emitterRepository.deleteByClientKey(key);

                }
            } catch (IOException e) {
                emitterRepository.deleteByClientKey(key);
            }
        });
    }

    public void pollQueue() {
        Long queueSize = queueRepository.size();
        if (queueSize == null || queueSize == 0L) {
            return;
        }
        for (int i=0; i<QUEUE_THROUGHPUT; i++) {
            String clientKey = queueRepository.poll();
            if (clientKey == null) {
                continue;
            }
            createToken(clientKey);
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

    /**
     * 예매 권한 토큰이 유효한지 검증
     * @param jwt JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean isValidReservationToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }

        try {
            // 1. JWT 파싱 및 서명/만료 검증
            var claims = tokenManager.parseAndValidate(jwt);
            String clientKey = claims.getSubject();

            // 2. Redis에 토큰 존재 여부 확인
            String storedToken = tokenRepository.get(clientKey);
            if (storedToken == null) {
                return false;  // 토큰 없음 (만료 or 이미 사용됨)
            }

            // 3. 토큰 일치 여부 확인
            return storedToken.equals(jwt);

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
}
