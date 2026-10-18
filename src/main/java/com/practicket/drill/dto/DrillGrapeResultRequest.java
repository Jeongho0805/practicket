package com.practicket.drill.dto;

import com.practicket.drill.domain.DrillSeatForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class DrillGrapeResultRequest {

    @NotNull
    private DrillSeatForm form;

    @NotNull
    @Size(min = 5, max = 5)
    private List<@Valid @NotNull Round> rounds;

    @Getter
    public static class Round {

        @NotNull
        private Integer seats;

        @NotNull
        @Min(100)
        @Max(600_000)
        private Integer elapsedMs;

        @NotNull
        @Min(0)
        @Max(1_000)
        private Integer misclicks;

        @NotNull
        @Min(0)
        private Integer soldClicks;
    }
}
