package com.practicket.ticket.scheduler;

import com.practicket.common.auth.ClientInfo;
import com.practicket.ticket.application.TicketQueueService;
import com.practicket.ticket.application.TicketService;
import com.practicket.ticket.component.VirtualNameLoader;
import com.practicket.ticket.dto.TicketRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Component
@RequiredArgsConstructor
public class TicketScheduler {

    private final TicketService ticketService;

    private final TicketQueueService ticketQueueService;

    private final VirtualNameLoader nameLoader;

    private final static int AI_USER_NUMBER = 200;

    @Scheduled(cron = "30 * * * * *")
    @SchedulerLock(name = "adjustStartTime")
    public void adjustStartTime() throws InterruptedException {
        ticketService.adjustStartTime();
    }

    @Scheduled(cron = "59 * * * * *")
    @SchedulerLock(name = "clearAllRecord")
    public void clearAllRecord() {
        ticketQueueService.initData();
        ticketService.initData();
    }

    @Scheduled(cron = "6 * * * * *")
    @SchedulerLock(name = "activateAIUserTicket")
    public void activateAIUserTicket() {
        List<String> shuffledList = new ArrayList<>(nameLoader.getNames());
        Collections.shuffle(shuffledList);
        int randomNumber = ThreadLocalRandom.current().nextInt(30, 60);
        int intervalTime = ThreadLocalRandom.current().nextInt(50, 200);
        for (int i=1; i<=randomNumber; i++) {
            try {
                Thread.sleep(intervalTime);
                String name = shuffledList.remove(0);
                int rowSize = 10;
                int colSize = 10;
                Random random = new Random();
                int row = random.nextInt(rowSize);
                int col = random.nextInt(colSize);
                String seat = (char) ('A' + row) + String.valueOf(col + 1);
                ClientInfo clientInfo = ClientInfo.builder()
                        .token(name)
                        .name(name)
                        .build();
                ticketService.createTicket(clientInfo, new TicketRequestDto(List.of(seat)));
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "addVirtualUserOnQueue")
    public void addVirtualUserOnQueue() {
        String name = "AI-User-";
        for (int i=1; i<=AI_USER_NUMBER; i++) {
            try {
                Thread.sleep(30);
                ticketQueueService.enterQueue(name + i);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "* * * * * *")
    @SchedulerLock(name = "pollQueue")
    public void pollQueue() {
        ticketQueueService.pollQueue();
    }

    @Scheduled(cron = "* * * * * *")
    public void broadcastQueueInfo() {
        ticketQueueService.broadcastQueueInfo();
    }
}
