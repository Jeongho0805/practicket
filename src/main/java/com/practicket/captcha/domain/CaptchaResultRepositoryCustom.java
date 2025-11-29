package com.practicket.captcha.domain;

import java.util.List;

public interface CaptchaResultRepositoryCustom {

    List<CaptchaResult> findAllByElapsedSecondLessThan(float threshold, long limit);

    List<CaptchaResult> findAllByClientId(Long clientId);
}
