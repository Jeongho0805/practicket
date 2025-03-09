package com.example.ticketing.captcha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class CaptchaCreateRequest {

    @NotNull
    @Positive
    private Float elapsedTime;
}
