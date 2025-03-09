package com.example.ticketing.captcha.component;

import com.example.ticketing.auth.component.ClientInfoManager;
import com.example.ticketing.common.domain.entity.CaptchaResult;
import com.example.ticketing.common.domain.entity.ClientInfo;
import com.example.ticketing.common.domain.repository.CaptchaResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CaptchaResultManager {

    private final CaptchaResultRepository captchaResultRepository;

    private final ClientInfoManager clientInfoManager;

    public void save(String sessionKey, Float elapsedTime) {
        ClientInfo clientInfo = clientInfoManager.findBySessionKey(sessionKey);
        captchaResultRepository.save(CaptchaResult.builder()
                .clientInfo(clientInfo)
                .elapsedSecond(elapsedTime)
                .build());
    }

    public List<Float> findAllElapsedTimes() {
        List<CaptchaResult> captchaResults = captchaResultRepository.findAllByElapsedSecondLessThan(30, 10000);
        return captchaResults.stream()
                .map(CaptchaResult::getElapsedSecond)
                .toList();
    }

    public List<Float> findAllMyElapsedTimes(String sessionKey) {
        ClientInfo clientInfo = clientInfoManager.findBySessionKey(sessionKey);
        List<CaptchaResult> captchaResults = captchaResultRepository.findAllByClientInfo(clientInfo);
        return captchaResults.stream()
                .map(CaptchaResult::getElapsedSecond)
                .toList();
    }
}
