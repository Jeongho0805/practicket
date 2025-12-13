package com.practicket.ticket.application;

import com.practicket.ticket.domain.TicketQueueEmitterRepository;
import com.practicket.ticket.domain.TicketQueueEventRepository;
import com.practicket.ticket.dto.TicketQueueEventDto;
import com.practicket.ticket.dto.TicketWaitingOrderResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TicketQueueService {

    private final int WORKER_COUNT = 10;

    private final TicketQueueEventRepository eventRepository;

    private final TicketQueueEmitterRepository emitterRepository;

    private final ThreadPoolTaskExecutor taskExecutor;

    private static final String WAITING_QUEUE_NAME = "waiting-order";

    public TicketQueueService(TicketQueueEventRepository eventRepository,
                              TicketQueueEmitterRepository emitterRepository,
                              @Qualifier("ticketTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.eventRepository = eventRepository;
        this.emitterRepository = emitterRepository;
        this.taskExecutor = executor;
    }

    @PostConstruct
    public void startProcessing() {
        for (int i=0; i<WORKER_COUNT; i++) {
            taskExecutor.execute(this::processQueue);
        }
    }

    public void processQueue()  {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            TicketQueueEventDto dto = null;
            try {
                dto = eventRepository.pollEvent();
            } catch (Exception e) {
                log.error("예외 발생 {}", e.getMessage());
            }
            if (dto == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void saveEvent(String key) {
        int currentTotalWaitingNumber = eventRepository.getListSize();
        TicketQueueEventDto eventDto = new TicketQueueEventDto(key, currentTotalWaitingNumber);
        eventRepository.pushEvent(eventDto);
    }

    public TicketWaitingOrderResponse getWaitingOrder(String key) {
        List<TicketQueueEventDto> events = eventRepository.findAllEvents();
        List<String> names = events.stream().map(TicketQueueEventDto::getName).toList();
        int index = names.indexOf(key);
        if (index == -1) {
            return new TicketWaitingOrderResponse(index, 0);
        }
        TicketQueueEventDto eventDto = events.get(index);
        return new TicketWaitingOrderResponse(index, eventDto.getFirstWaitingOrder());
    }

    public void deleteAllWaiting() {
        eventRepository.deleteAll();
    }

    public SseEmitter saveEmitter(String key) {
        SseEmitter emitter = new SseEmitter(0L);
        emitterRepository.save(key, emitter);
        emitter.onCompletion(() -> emitterRepository.deleteByName(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterRepository.deleteByName(key);
        });
        emitter.onError((error) -> {
            emitter.complete();
            emitterRepository.deleteByName(key);
        });
        return emitter;
    }

    public void sendOrderByEmitter() {
        Map<String, SseEmitter> emitters = emitterRepository.findAll();
        if (emitters.isEmpty()) {
            return;
        }
        for (String key : emitters.keySet()) {
            TicketWaitingOrderResponse data = getWaitingOrder(key);
            SseEmitter emitter = emitters.get(key);
            if (emitter == null) continue;
            try{
                if (isEmitterActive(emitter)) {
                    emitter.send(SseEmitter.event()
                            .name(WAITING_QUEUE_NAME)
                            .data(data));
                }
            } catch (Exception e){
                emitterRepository.deleteByName(key);
            }
        }
    }

    private boolean isEmitterActive(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name(WAITING_QUEUE_NAME));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
