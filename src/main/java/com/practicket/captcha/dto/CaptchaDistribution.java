package com.practicket.captcha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaptchaDistribution {

    private float lowerBound;

    private float binWidth;

    private long[] counts;

    private int myBin;
}
