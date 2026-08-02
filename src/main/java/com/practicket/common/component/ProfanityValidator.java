package com.practicket.common.component;

import com.practicket.api.profanity.ProfanityApiClient;
import com.practicket.api.profanity.dto.ValidationResponse;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityValidator {

    private final ProfanityApiClient client;

    private final static double THRESHOLD = 0.7;

    /**
     * ProfanityApiClient 가 이미 1초에서 끊지만, block() 에도 한 번 더 건다.
     * 클라이언트 쪽 타임아웃이 언젠가 지워지면 이 호출이 무한정 매달리는데,
     * 그때 멈추는 건 욕설 판정이 아니라 글쓰기 전체다.
     */
    private final static Duration BLOCK_TIMEOUT = Duration.ofSeconds(2);

    public void validateProfanityText(String text) {
        ValidationResponse response = null;
        try {
            response = client.validate(text).block(BLOCK_TIMEOUT);
        } catch (Exception e) {
            // fail-open. 필터가 죽었다고 글쓰기가 멈추는 게 욕설 하나 새는 것보다 나쁘다(Q6).
            log.error("Profanity API 호출 실패 : 통과된 text={}, exception={}", text, e.getMessage(), e);
        }
        if (response != null && response.getIsProfanity() && response.getConfidence() > THRESHOLD) {
            throw new ValidateException(ErrorCode.INAPPROPRIATE_CONTENT);
        }
    }
}
