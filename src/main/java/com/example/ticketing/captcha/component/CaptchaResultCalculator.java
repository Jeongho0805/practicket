package com.example.ticketing.captcha.component;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaptchaResultCalculator {

    public float calAverageElapsedTime(List<Float> elapsedTimes) {
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

    public float calLatestPercentile(List<Float> totalResults, List<Float> myResults) {
        if (myResults.isEmpty()) {
            return -1;
        }
        Float myElapsedTime = myResults.get(0);
        long betterCount = totalResults.stream()
                .filter(r -> r < myElapsedTime)
                .count();
        return (betterCount * 100f) / totalResults.size();
    }

    public float calAvgPercentile(List<Float> totalResults, List<Float> myResults) {
        if (myResults.isEmpty()) {
            return -1;
        }
        float sum = (float) myResults.stream()
                .mapToDouble(Float::doubleValue)
                .sum();
        Float myAvgElapsedTime = sum / myResults.size();
        long betterCount = totalResults.stream()
                .filter(r -> r < myAvgElapsedTime)
                .count();
        return (betterCount * 100f) / totalResults.size();
    }

    public float extractMyBestResult(List<Float> myResults) {
        if (myResults.isEmpty()) {
            return 0;
        }
        return myResults.stream().sorted().toList().get(0);
    }
}
