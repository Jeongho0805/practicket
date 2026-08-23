package com.practicket.captcha.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 어느 파드에서 기록이 저장되든 모든 파드의 캐시를 함께 비운다.
 * 이게 없으면 기록을 낸 사람이 다른 파드로 붙었을 때 최대 1분간 옛 통계를 본다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaStatInvalidationSubscriber {

    private final CaptchaGlobalStatCache captchaGlobalStatCache;

    public void onMessage(String message) {
        try {
            captchaGlobalStatCache.invalidateLocal();
        } catch (Exception e) {
            log.error("캡차 통계 캐시 무효화 처리 실패", e);
        }
    }
}
