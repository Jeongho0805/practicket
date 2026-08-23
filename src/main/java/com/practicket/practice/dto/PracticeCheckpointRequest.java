package com.practicket.practice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PracticeCheckpointRequest {

    @NotBlank
    private String sessionId;
}
