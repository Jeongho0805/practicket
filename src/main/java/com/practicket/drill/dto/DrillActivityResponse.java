package com.practicket.drill.dto;

public record DrillActivityResponse(long players, long rounds, Drill react, Drill captcha, Drill grape, Drill cancel) {

    public record Drill(long players, long rounds) {
    }
}
