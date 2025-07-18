package com.example.ticketing.captcha.component;

import com.example.ticketing.captcha.domain.CaptchaResult;
import com.example.ticketing.captcha.domain.CaptchaResultRepository;
import com.example.ticketing.client.domain.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CaptchaResultManager {

    private final CaptchaResultRepository captchaResultRepository;

    public void save(Client client, Float elapsedTime) {
        captchaResultRepository.save(CaptchaResult.builder()
                .client(client)
                .elapsedSecond(elapsedTime)
                .build());
    }

    public List<Float> findAllElapsedTimes() {
        List<CaptchaResult> captchaResults = captchaResultRepository.findAllByElapsedSecondLessThan(30, 10000);
        return captchaResults.stream()
                .map(CaptchaResult::getElapsedSecond)
                .toList();
    }

    public List<Float> findAllMyElapsedTimes(Long clientId) {
        List<CaptchaResult> captchaResults = captchaResultRepository.findAllByClientId(clientId);
        return captchaResults.stream()
                .map(CaptchaResult::getElapsedSecond)
                .toList();
    }
}
