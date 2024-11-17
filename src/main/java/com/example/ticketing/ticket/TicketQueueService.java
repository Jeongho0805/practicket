package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketQueueEventDto;
import com.example.ticketing.ticket.dto.OrderRequestDto;
import com.example.ticketing.ticket.dto.TicketWaitingOrderResponse;
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

    private final int WORKER_COUNT = 5;

    private final TicketQueueEventRepository eventRepository;

    private final TicketQueueEmitterRepository emitterRepository;

    private final ThreadPoolTaskExecutor taskExecutor;

    private final TicketService ticketService;

    public TicketQueueService(TicketQueueEventRepository eventRepository,
                              TicketQueueEmitterRepository emitterRepository,
                              @Qualifier("ticketTaskExecutor") ThreadPoolTaskExecutor executor,
                              TicketService ticketService) {
        this.eventRepository = eventRepository;
        this.emitterRepository = emitterRepository;
        this.taskExecutor = executor;
        this.ticketService = ticketService;
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

    public void saveEvent(OrderRequestDto dto) {
        int currentTotalWaitingNumber = eventRepository.getListSize();
        TicketQueueEventDto eventDto = new TicketQueueEventDto(dto.getName(), currentTotalWaitingNumber);
        eventRepository.pushEvent(eventDto);
    }

    public TicketWaitingOrderResponse getWaitingOrder(String name) {
        List<TicketQueueEventDto> events = eventRepository.findAllEvents();
        List<String> names = events.stream().map(TicketQueueEventDto::getName).toList();
        int index = names.indexOf(name);
        if (index == -1) {
            return new TicketWaitingOrderResponse(index, 0);
        }
        TicketQueueEventDto eventDto = events.get(index);
        return new TicketWaitingOrderResponse(index, eventDto.getFirstWaitingOrder());
    }

    public void deleteAllWaiting() {
        eventRepository.deleteAll();
    }

    public SseEmitter saveEmitter(String name) {
        SseEmitter emitter = new SseEmitter(0L);
        emitterRepository.save(name, emitter);
        emitter.onCompletion(() -> emitterRepository.deleteByName(name));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitterRepository.deleteByName(name);
        });
        emitter.onError((error) -> {
            emitter.complete();
            emitterRepository.deleteByName(name);
        });
        return emitter;
    }

    public void sendOrderByEmitter() {
        Map<String, SseEmitter> emitters = emitterRepository.findAll();
        if (emitters.isEmpty()) {
            return;
        }
        for (String name : emitters.keySet()) {
            TicketWaitingOrderResponse data = getWaitingOrder(name);
            SseEmitter emitter = emitters.get(name);
            if (emitter == null) continue;
            try{
                if (isEmitterActive(emitter)) {
                    emitter.send(SseEmitter.event()
                            .name("waiting-order")
                            .data(data));
                }
            } catch (Exception e){
                emitterRepository.deleteByName(name);
            }
        }
    }

    private boolean isEmitterActive(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("waiting-order"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
