package com.example.ticketing.common.component;

import com.example.ticketing.api.profanity.ProfanityApiClient;
import com.example.ticketing.api.profanity.dto.ValidationResponse;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.ValidateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityValidator {

    private final ProfanityApiClient client;

    private final static double THRESHOLD = 0.7;

    public void validateProfanityText(String text) {
        ValidationResponse response = null;
        try {
            response = client.validate(text).block();
        } catch (Exception e) {
            log.error("Profanity API 호출 실패 : 통과된 text={}, exception={}", text, e.getMessage(), e);
        }
        if (response != null && response.getIsProfanity() && response.getConfidence() > THRESHOLD) {
            throw new ValidateException(ErrorCode.INAPPROPRIATE_CONTENT);
        }
    }
}
