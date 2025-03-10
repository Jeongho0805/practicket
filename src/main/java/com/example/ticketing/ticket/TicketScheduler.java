package com.example.ticketing.ticket;

import com.example.ticketing.ticket.component.TicketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Component
@RequiredArgsConstructor
public class TicketScheduler {

    private final TicketService ticketService;

    private final TicketQueueService ticketQueueService;

    private final TicketManager ticketManager;

    private static final List<String> nameList = List.of(
            "카리나 존예", "윈터보고싶다ㅏㅏㅏ", "닝닝닝닝", "에스파가즈아아", "보넥도",
            "하이하니이", "해린쓰", "하니얌", "존예민지", "조슈아",
            "민규우", "카즈하", "사꾸랑", "막둥이은채", "채원이",
            "꼭간다", "안유진", "젭알", "지디", "티켓팅ㅇㅇㅇ",
            "ㅁㄴㅇㄹ", "시은", "윤이최공", "세은빛나라", "아이랜드더보이즈",
            "모라카농리ㅏㅁ", "ㄹㅈㄷ", "한빈아ㅜㅜ", "방탄", "유진언니",
            "도겸사랑해", "최예나짱", "태용존잘", "엔하이픈", "케플러닷",
            "원영사랑해", "가고싶다아아", "루세라핌짱", "제로베라사랑해", "이서이",
            "aaa", "갓채원", "dkfjdk", "제노내꼬", "해린볼살",
            "강산", "가고싶다ㅜㅜㅜ", "이선좌ㅏㅏㅏ", "뉴찐쓰", "콘가즈아",
            "유에낭", "아이유", "티켓팅존망", "IU", "마나러",
            "덕질덕지", "티케팅뿌셩", "차으늉", "으뉴야사랑해", "다비켜",
            "덕후의삶", "누구냥", "아놔", "원영아기다료", "호우호우",
            "팬싸가고싶다", "hhh", "최애콘", "방ㅋ탄ㅌ콘", "직관각",
            "스밍중", "티켓팅달인", "너무긴장돼", "팬싸성공", "운댕",
            "리사", "덕질", "최예나사랑해", "할뚜이따", "ㅎㅇㅎㅇ",
            "호시탐탐", "굿즈사고싶다", "태용내꼬", "ㅇㅈ", "팬질중독",
            "최애직캠존버", "츌첵", "덕질플랜짜는중", "팬싸고고", "스밍돌리는중",
            "딱기다려라", "오늘성공", "아이돌의길", "덕후하루시작", "티켓팅전쟁",
            "워누야기다려", "오우", "ㄷ가ㅓ아", "winter", "ㅋㅋㅋㅋㅋ"
    );

    private final static int AI_USER_NUMBER = 300;

    private final static int AI_USER_INTERVAL = 100;

    @Scheduled(cron = "30 * * * * *")
    public void resetTime() {
        ticketService.resetTimer();
    }

    @Scheduled(cron = "59 * * * * *")
    public void clearAllRecord() {
        ticketQueueService.deleteAllWaiting();
        ticketService.initData();
    }

    @Scheduled(cron = "0 * * * * *")
    public void activateAIUserOrder() {
        String name = "AI-User-";
        for (int i=1; i<=AI_USER_NUMBER; i++) {
            try {
                Thread.sleep(30);
                ticketQueueService.saveEvent(name + i);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "6 * * * * *")
    public void activateAIUserTicket() {
        List<String> shuffledList = new ArrayList<>(nameList);
        Collections.shuffle(shuffledList);
        int randomNumber = ThreadLocalRandom.current().nextInt(80, 101);
        for (int i=1; i<=randomNumber; i++) {
            try {
                Thread.sleep(AI_USER_INTERVAL);
                String name = shuffledList.remove(0);
                ticketManager.createTicketForAI(name);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "* * * * * *")
    public void sendWaitingOrder() {
        ticketQueueService.sendOrderByEmitter();
    }
}
