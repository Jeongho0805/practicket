package com.practicket.captcha.component;

import com.practicket.captcha.domain.CaptchaResult;
import com.practicket.captcha.domain.CaptchaResultRepository;
import com.practicket.captcha.dto.CaptchaMyStat;
import com.practicket.captcha.dto.CaptchaRankRow;
import com.practicket.client.domain.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CaptchaResultManager {

    private final CaptchaResultRepository captchaResultRepository;

    public void save(Client client, Float elapsedTime) {
        captchaResultRepository.save(CaptchaResult.builder()
                .client(client)
                .elapsedSecond(elapsedTime)
                .build());
    }

    public long countAll() {
        return captchaResultRepository.countAll();
    }

    public float averageAll() {
        return captchaResultRepository.averageAll();
    }

    public long countFasterThan(float elapsedSecond) {
        return captchaResultRepository.countFasterThan(elapsedSecond);
    }

    public float quantile(double ratio) {
        return captchaResultRepository.quantile(ratio);
    }

    public long[] histogram(float lowerBound, float binWidth, int binCount) {
        return captchaResultRepository.histogram(lowerBound, binWidth, binCount);
    }

    public List<CaptchaRankRow> findTopRanking(LocalDateTime from, int limit) {
        return captchaResultRepository.findTopRanking(from, limit);
    }

    public long countPeopleSince(LocalDateTime from) {
        return captchaResultRepository.countPeopleSince(from);
    }

    public CaptchaMyStat findMyStat(Long clientId) {
        return captchaResultRepository.findMyStat(clientId);
    }

    public List<Float> findRecentElapsedSeconds(Long clientId, int limit) {
        return captchaResultRepository.findRecentElapsedSeconds(clientId, limit);
    }
}
