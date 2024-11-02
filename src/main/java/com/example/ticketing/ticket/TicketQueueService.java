package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketRequestDto;
import com.example.ticketing.ticket.dto.TicketWaitingInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TicketQueueService {

    private final int WORKER_COUNT = 5;

    private final TicketQueueRepository ticketQueueRepository;

    private final ThreadPoolTaskExecutor taskExecutor;

    private final TicketService ticketService;

    public TicketQueueService(TicketQueueRepository ticketQueueRepository,
                              @Qualifier("ticketTaskExecutor") ThreadPoolTaskExecutor executor,
                              TicketService ticketService) {
        this.ticketQueueRepository = ticketQueueRepository;
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
            TicketRequestDto dto = ticketQueueRepository.pollEvent();
            if (dto != null) {
                ticketService.issueTicket(dto.getName());
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void saveEvent(TicketRequestDto dto) {
        ticketQueueRepository.pushEvent(dto);
    }

    public TicketWaitingInfo getWaitingInfo(String name) {
        List<String> names = ticketQueueRepository.findAllNameList();
        return new TicketWaitingInfo(names.indexOf(name), names.size());
    }

    public void deleteAllWaiting() {
        ticketQueueRepository.deleteAll();
    }
}
