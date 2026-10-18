package com.practicket.drill.application;

import com.practicket.drill.domain.DrillCaptchaStat;
import com.practicket.drill.domain.DrillCaptchaStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 첫 판의 누적 행은 따로 커밋해야 바깥 트랜잭션이 그 행을 잠글 수 있다. */
@Component
@RequiredArgsConstructor
public class DrillCaptchaStatCreator {

    private final DrillCaptchaStatRepository drillCaptchaStatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfAbsent(Long clientId) {
        if (!drillCaptchaStatRepository.existsByClientId(clientId)) {
            drillCaptchaStatRepository.saveAndFlush(DrillCaptchaStat.empty(clientId));
        }
    }
}
