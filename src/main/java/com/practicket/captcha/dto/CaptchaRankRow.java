package com.practicket.captcha.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaptchaRankRow {

    @JsonIgnore
    private Long clientId;

    private String nickname;

    private float elapsedSecond;

    private boolean mine;
}
