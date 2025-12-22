package com.practicket.ticket.application;

import com.practicket.ticket.dto.TicketWaitingOrderResponse;
import com.practicket.ticket.infra.redis.TicketQueueRepository;
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
            TicketWaitingOrderResponse ticketWaitingOrder = new TicketWaitingOrderResponse(currentRank, initialRank);
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
            queueRepository.poll();
        }
    }
}
