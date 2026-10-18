package com.practicket.drill.dto;

import com.practicket.drill.domain.DrillSeatForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/** 유형별 범위(일반형 100ms 하한, 구간 합 등)는 서비스에서 따로 본다. */
@Getter
public class DrillCancelResultRequest {

    @NotNull
    private DrillSeatForm form;

    @NotNull
    @Size(min = 5, max = 5)
    private List<@Valid @NotNull Round> rounds;

    @Getter
    public static class Round {

        @NotNull
        @Min(50)
        @Max(600_000)
        private Integer elapsedMs;

        @NotNull
        @Min(0)
        private Integer discoverMs;

        @NotNull
        @Min(1)
        private Integer selectMs;

        @NotNull
        @Min(0)
        @Max(1_000)
        private Integer misclicks;

        @NotNull
        @Min(0)
        private Integer soldClicks;

        @NotNull
        @Min(0)
        @Max(100)
        private Integer missed;
    }
}
