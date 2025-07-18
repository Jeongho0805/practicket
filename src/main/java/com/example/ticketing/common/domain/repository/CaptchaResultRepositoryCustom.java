package com.example.ticketing.common.domain.repository;

import com.example.ticketing.client.domain.Client;
import com.example.ticketing.common.domain.entity.CaptchaResult;

import java.util.List;

public interface CaptchaResultRepositoryCustom {

    List<CaptchaResult> findAllByElapsedSecondLessThan(float threshold, long limit);

    List<CaptchaResult> findAllByClientInfo(Client clientInfo);
}
