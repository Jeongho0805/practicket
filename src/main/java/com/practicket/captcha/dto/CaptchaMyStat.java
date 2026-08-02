package com.practicket.captcha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaptchaMyStat {

    private long count;

    private float best;

    private float average;

    private float latest;
}
