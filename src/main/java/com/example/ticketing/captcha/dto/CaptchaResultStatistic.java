package com.example.ticketing.captcha.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaptchaResultStatistic {

    private float latestResult;

    private float myAvgResult;

    private float totalAvgResult;
}
