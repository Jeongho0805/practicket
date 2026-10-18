package com.practicket.drill.dto;

import java.util.List;

public record DrillCaptchaTypoStatsResponse(List<Letter> letters, int imeCount) {

    public record Letter(String letter, int seen, int slots, int miss, List<Typed> typed) {
    }

    public record Typed(String letter, int count) {
    }
}
