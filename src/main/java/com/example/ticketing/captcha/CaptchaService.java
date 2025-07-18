//package com.example.ticketing.captcha;
//
//import com.example.ticketing.captcha.component.CaptchaResultCalculator;
//import com.example.ticketing.captcha.component.CaptchaResultManager;
//import com.example.ticketing.captcha.dto.CaptchaCreateRequest;
//import com.example.ticketing.captcha.dto.CaptchaResultStatistic;
//import com.example.ticketing.common.auth.ClientInfo;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class CaptchaService {
//
//    private final CaptchaResultManager captchaResultManager;
//
//    private final CaptchaResultCalculator captchaResultCalculator;
//
//    public CaptchaResultStatistic getStatistic(ClientInfo userInfo) {
//        List<Float> totalResults = captchaResultManager.findAllElapsedTimes();
//        List<Float> myResults = captchaResultManager.findAllMyElapsedTimes(userInfo.getToken());
//        return CaptchaResultStatistic.builder()
//                .latestResult(captchaResultCalculator.extractFirstIndexValue(myResults))
//                .myAvgResult(captchaResultCalculator.getAverageElapsedTime(myResults))
//                .totalAvgResult(captchaResultCalculator.getAverageElapsedTime(totalResults))
//                .build();
//    }
//
//    public void createResult(ClientInfo userInfo, CaptchaCreateRequest requestDto) {
//        captchaResultManager.save(userInfo.getToken(), requestDto.getElapsedTime());
//    }
//}
