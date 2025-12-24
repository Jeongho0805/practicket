package com.practicket.ticket.scheduler;

import com.practicket.common.auth.ClientInfo;
import com.practicket.ticket.application.TicketQueueService;
import com.practicket.ticket.application.TicketService;
import com.practicket.ticket.component.VirtualNameLoader;
import com.practicket.ticket.domain.TicketToken;
import com.practicket.ticket.dto.request.TicketRequestDto;
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

    private final static int VIRTUAL_USER_COUNT = 200;

    private static final String VIRTUAL_USER_PREFIX = "VIRTUAL-USER-";

    @Scheduled(cron = "* * * * * *")
    @SchedulerLock(name = "pollQueue")
    public void pollQueue() {
        ticketQueueService.pollQueue();
    }

    @Scheduled(cron = "* * * * * *")
    public void broadcastQueueInfo() {
        ticketQueueService.broadcastQueueInfo();
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "addVirtualUserOnQueue")
    public void addVirtualUserOnQueue() {
        for (int i = 1; i<= VIRTUAL_USER_COUNT; i++) {
            try {
                Thread.sleep(30);
                ticketQueueService.enterQueue(VIRTUAL_USER_PREFIX + i);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "6 * * * * *")
    @SchedulerLock(name = "issueTicketForVirtualUser")
    public void issueTicketForVirtualUser() {
        List<String> shuffledList = new ArrayList<>(nameLoader.getNames());
        Collections.shuffle(shuffledList);
        int randomNumber = ThreadLocalRandom.current().nextInt(VIRTUAL_USER_COUNT - 50, VIRTUAL_USER_COUNT);
        int intervalTime = ThreadLocalRandom.current().nextInt(50, 200);
        for (int i=1; i<=randomNumber; i++) {
            try {
                Thread.sleep(intervalTime);
                String name = shuffledList.remove(0);
                // 토큰 생성
                String virtualUserClientKey = VIRTUAL_USER_PREFIX + i;
                TicketToken token = ticketQueueService.createToken(virtualUserClientKey);
                if (token == null) {
                    continue;
                }
                // 가상 유저 정보 세팅
                ClientInfo clientInfo = ClientInfo.builder()
                        .token(virtualUserClientKey)
                        .name(name)
                        .build();
                // 예매 처리
                String seat = generateRandomSeat();
                ticketService.createTicket(clientInfo, new TicketRequestDto(List.of(seat), token.getJwt()));
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "30 * * * * *")
    @SchedulerLock(name = "adjustStartTime")
    public void adjustStartTime() {
        ticketService.adjustStartTime();
    }

    @Scheduled(cron = "59 * * * * *")
    @SchedulerLock(name = "clearAllRecord")
    public void clearAllRecord() {
        ticketQueueService.initData();
        ticketService.initData();
    }

    private String generateRandomSeat() {
        Random random = new Random();
        int row = random.nextInt(10);
        int col = random.nextInt(10);
        return (char) ('A' + row) + String.valueOf(col + 1);
    }
}
