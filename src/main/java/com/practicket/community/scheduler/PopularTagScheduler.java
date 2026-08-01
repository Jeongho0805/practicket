package com.practicket.community.scheduler;

import com.practicket.community.application.PopularTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 파드가 여러 개여도 한 대만 돌면 되므로 ShedLock 을 건다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularTagScheduler {

    private final PopularTagService popularTagService;

    @Scheduled(cron = "0 5 * * * *")
    @SchedulerLock(name = "refreshPopularTags")
    public void refreshPopularTags() {
        try {
            popularTagService.refresh();
        } catch (Exception e) {
            // 갱신이 실패해도 게시판은 돌아간다
            log.warn("인기 태그 갱신 실패", e);
        }
    }
}
