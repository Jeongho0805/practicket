package com.example.ticketing.captcha.component;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaptchaResultCalculator {

    public float getAverageElapsedTime(List<Float> elapsedTimes) {
        if (elapsedTimes.isEmpty()) {
            return 0;
        }
        float sum = (float) elapsedTimes.stream()
                .mapToDouble(Float::doubleValue) // Float → double 변환
                .sum();
        return sum / elapsedTimes.size();
    }

    public float extractFirstIndexValue(List<Float> elapsedTimes) {
        if (elapsedTimes.isEmpty()) {
            return 0;
        }
        return elapsedTimes.get(0);
    }
}
