package com.practicket.drill.dto;

import com.practicket.drill.domain.DrillReactForm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class DrillReactResultRequest {

    @NotNull
    private DrillReactForm form;

    @NotNull
    @Min(1)
    @Max(20_000)
    private Integer reactionMs;

    @Min(10)
    @Max(5_000)
    private Integer latencyMs;
}
