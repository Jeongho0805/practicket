package com.example.ticketing.common.domain.repository;

import com.example.ticketing.common.domain.entity.CaptchaResult;
import com.example.ticketing.client.domain.Client;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

import static com.example.ticketing.common.domain.entity.QCaptchaResult.captchaResult;


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

    @Override
    public List<CaptchaResult> findAllByClientInfo(Client clientInfo) {
        return queryFactory
                .selectFrom(captchaResult)
                .where(captchaResult.clientInfo.eq(clientInfo))
                .orderBy(captchaResult.createdAt.desc())
                .fetch();
    }
}
