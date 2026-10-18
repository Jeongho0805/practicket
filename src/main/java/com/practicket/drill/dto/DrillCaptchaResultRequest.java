package com.practicket.drill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class DrillCaptchaResultRequest {

    public static final String IME = "한";

    @NotNull
    @Size(min = 3, max = 3)
    private List<@Valid @NotNull Problem> problems;

    @Getter
    public static class Problem {

        @NotNull
        @Pattern(regexp = "^[A-HJ-NP-Z]{6}$")
        private String answer;

        @NotNull
        @Min(300)
        @Max(60_000)
        private Integer elapsedMs;

        @NotNull
        @Size(max = 30)
        private List<@Valid @NotNull Typo> typos;
    }

    @Getter
    public static class Typo {

        @NotNull
        @Min(0)
        @Max(5)
        private Integer position;

        @NotNull
        @Pattern(regexp = "^([A-Z]|" + IME + ")$")
        private String typed;
    }
}
