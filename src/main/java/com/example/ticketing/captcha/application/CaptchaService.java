package com.example.ticketing.captcha;

import com.example.ticketing.captcha.component.CaptchaResultCalculator;
import com.example.ticketing.captcha.component.CaptchaResultManager;
import com.example.ticketing.captcha.dto.CaptchaCreateRequest;
import com.example.ticketing.captcha.dto.CaptchaResultStatistic;
import com.example.ticketing.client.component.ClientManager;
import com.example.ticketing.client.domain.Client;
import com.example.ticketing.common.auth.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaResultManager captchaResultManager;

    private final CaptchaResultCalculator captchaResultCalculator;

    private final ClientManager clientManager;

    @Transactional(readOnly = true)
    public CaptchaResultStatistic getStatistic(ClientInfo clientInfo) {
        List<Float> totalResults = captchaResultManager.findAllElapsedTimes();
        List<Float> myResults = captchaResultManager.findAllMyElapsedTimes(clientInfo.getClientId());

        return CaptchaResultStatistic.builder()
                .latestRank(captchaResultCalculator.extractMyLatestRank(totalResults, myResults))
                .latestResult(captchaResultCalculator.extractFirstIndexValue(myResults))
                .myAvgResult(captchaResultCalculator.getAverageElapsedTime(myResults))
                .totalAvgResult(captchaResultCalculator.getAverageElapsedTime(totalResults))
                .build();
    }

    public void createResult(ClientInfo clientInfo, CaptchaCreateRequest requestDto) {
        Client client = clientManager.findById(clientInfo.getClientId());
        captchaResultManager.save(client, requestDto.getElapsedTime());
    }
}
