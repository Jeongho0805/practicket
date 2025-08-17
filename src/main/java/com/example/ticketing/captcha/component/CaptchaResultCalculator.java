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

    public int extractMyLatestRank(List<Float> totalResults, List<Float> myResults) {
        if (myResults.isEmpty()) {
            return -1;
        }
        List<Float> totalResultSortByElapsedTime = totalResults.stream().sorted().toList();
        float latestResult = myResults.get(0);

        for (int i = 0; i < totalResultSortByElapsedTime.size(); i++) {
            if (Float.compare(latestResult, totalResultSortByElapsedTime.get(i)) <= 0) {
                return i + 1;
            }
        }
        return totalResults.size();
    }
}
