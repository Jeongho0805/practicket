package com.example.ticketing.captcha;

import com.example.ticketing.captcha.component.CaptchaResultCalculator;
import com.example.ticketing.captcha.component.CaptchaResultManager;
import com.example.ticketing.captcha.dto.CaptchaCreateRequest;
import com.example.ticketing.captcha.dto.CaptchaResultStatistic;
import com.example.ticketing.common.auth.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaResultManager captchaResultManager;

    private final CaptchaResultCalculator captchaResultCalculator;

    public CaptchaResultStatistic getStatistic(UserInfo userInfo) {
        List<Float> totalResults = captchaResultManager.findAllElapsedTimes();
        List<Float> myResults = captchaResultManager.findAllMyElapsedTimes(userInfo.getKey());
        return CaptchaResultStatistic.builder()
                .latestResult(captchaResultCalculator.extractFirstIndexValue(myResults))
                .myAvgResult(captchaResultCalculator.getAverageElapsedTime(myResults))
                .totalAvgResult(captchaResultCalculator.getAverageElapsedTime(totalResults))
                .build();
    }

    public void createResult(UserInfo userInfo, CaptchaCreateRequest requestDto) {
        captchaResultManager.save(userInfo.getKey(), requestDto.getElapsedTime());
    }
}
