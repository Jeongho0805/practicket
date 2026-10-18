package com.practicket.drill.dto;

import com.practicket.drill.domain.DrillReactForm;

import java.util.List;

public record DrillSummaryResponse(React react, Captcha captcha) {

    public record React(long count, List<Best> bests) {
    }

    public record Best(DrillReactForm form, Integer bestMs) {
    }

    public record Captcha(long count, Integer bestMs) {
    }
}
