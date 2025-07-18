package com.example.ticketing.captcha.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

import static com.example.ticketing.captcha.domain.QCaptchaResult.*;


@Repository
@RequiredArgsConstructor
public class CaptchaResultRepositoryCustomImpl implements CaptchaResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CaptchaResult> findAllByElapsedSecondLessThan(float threshold, long limit) {
        return queryFactory
                .selectFrom(captchaResult)
                .where(captchaResult.elapsedSecond.lt(threshold))
                .orderBy(captchaResult.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    public List<CaptchaResult> findAllByClientId(Long clientId) {
        return queryFactory
                .selectFrom(captchaResult)
                .where(captchaResult.client.id.eq(clientId))
                .orderBy(captchaResult.createdAt.desc())
                .fetch();
    }
}
