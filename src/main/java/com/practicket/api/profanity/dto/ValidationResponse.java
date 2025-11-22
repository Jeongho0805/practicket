package com.practicket.api.profanity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ValidationResponse {

    private String text;

    @JsonProperty("is_profanity")
    private Boolean isProfanity;

    private double confidence;
}
