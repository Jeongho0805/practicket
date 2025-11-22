package com.practicket.captcha.application;

import com.practicket.captcha.component.CaptchaResultCalculator;
import com.practicket.captcha.component.CaptchaResultManager;
import com.practicket.captcha.dto.CaptchaCreateRequest;
import com.practicket.captcha.dto.CaptchaResultStatistic;
import com.practicket.client.component.ClientManager;
import com.practicket.client.domain.Client;
import com.practicket.common.auth.ClientInfo;
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
                .myAvgResult(captchaResultCalculator.calAverageElapsedTime(myResults))
                .totalAvgResult(captchaResultCalculator.calAverageElapsedTime(totalResults))
                .latestPercentile(captchaResultCalculator.calLatestPercentile(totalResults, myResults))
                .avgPercentile(captchaResultCalculator.calAvgPercentile(totalResults, myResults))
                .bestResult(captchaResultCalculator.extractMyBestResult(myResults))
                .build();
    }

    public void createResult(ClientInfo clientInfo, CaptchaCreateRequest requestDto) {
        Client client = clientManager.findById(clientInfo.getClientId());
        captchaResultManager.save(client, requestDto.getElapsedTime());
    }
}
