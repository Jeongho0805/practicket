package com.practicket.drill.dto;

public record DrillActivityResponse(long players, long rounds, Drill react, Drill captcha) {

    public record Drill(long players, long rounds) {
    }
}
