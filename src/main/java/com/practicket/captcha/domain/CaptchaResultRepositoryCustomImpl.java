package com.practicket.captcha.domain;

import com.practicket.captcha.dto.CaptchaMyStat;
import com.practicket.captcha.dto.CaptchaRankRow;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.practicket.captcha.domain.QCaptchaResult.captchaResult;
import static com.practicket.client.domain.QClient.client;

@Repository
@RequiredArgsConstructor
public class CaptchaResultRepositoryCustomImpl implements CaptchaResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countAll() {
        Long count = queryFactory
                .select(captchaResult.count())
                .from(captchaResult)
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public float averageAll() {
        Double average = queryFactory
                .select(captchaResult.elapsedSecond.avg())
                .from(captchaResult)
                .fetchOne();
        return average == null ? 0f : average.floatValue();
    }

    @Override
    public long countFasterThan(float elapsedSecond) {
        Long count = queryFactory
                .select(captchaResult.count())
                .from(captchaResult)
                .where(captchaResult.elapsedSecond.lt(elapsedSecond))
                .fetchOne();
        return count == null ? 0 : count;
    }

    /** 탭을 켜둔 채 방치한 몇천 초짜리 기록 하나가 축 전체를 늘려버려서 min/max 대신 쓴다 */
    @Override
    public float quantile(double ratio) {
        long total = countAll();
        if (total == 0) {
            return 0f;
        }

        long offset = Math.min((long) (total * ratio), total - 1);
        Float value = queryFactory
                .select(captchaResult.elapsedSecond)
                .from(captchaResult)
                .orderBy(captchaResult.elapsedSecond.asc())
                .offset(offset)
                .limit(1)
                .fetchOne();

        return value == null ? 0f : value;
    }

    @Override
    public long[] histogram(float lowerBound, float binWidth, int binCount) {
        long[] counts = new long[binCount];
        if (binWidth <= 0) {
            return counts;
        }

        NumberExpression<Integer> bin = Expressions.numberTemplate(Integer.class,
                "floor(({0} - {1}) / {2})", captchaResult.elapsedSecond, lowerBound, binWidth);

        List<Tuple> rows = queryFactory
                .select(bin, captchaResult.count())
                .from(captchaResult)
                .groupBy(bin)
                .fetch();

        for (Tuple row : rows) {
            Integer rawBin = row.get(bin);
            Long count = row.get(captchaResult.count());
            if (rawBin == null || count == null) {
                continue;
            }
            counts[clampBin(rawBin, binCount)] += count;
        }
        return counts;
    }

    /** 범위 밖 기록을 버리면 칸 인원 합이 전체 건수와 어긋나므로 양 끝 칸에 담는다 */
    private int clampBin(int rawBin, int binCount) {
        return Math.max(0, Math.min(binCount - 1, rawBin));
    }

    @Override
    public List<CaptchaRankRow> findTopRanking(LocalDateTime from, int limit) {
        List<Tuple> rows = queryFactory
                .select(client.id, client.name, captchaResult.elapsedSecond.min())
                .from(captchaResult)
                .join(captchaResult.client, client)
                .where(from == null ? null : captchaResult.createdAt.goe(from))
                .groupBy(client.id, client.name)
                .orderBy(captchaResult.elapsedSecond.min().asc())
                .limit(limit)
                .fetch();

        List<CaptchaRankRow> ranking = new ArrayList<>();
        for (Tuple row : rows) {
            String nickname = row.get(client.name);
            Float best = row.get(captchaResult.elapsedSecond.min());
            ranking.add(new CaptchaRankRow(
                    row.get(client.id),
                    nickname == null || nickname.isBlank() ? "익명" : nickname,
                    best == null ? 0f : best,
                    false));
        }
        return ranking;
    }

    @Override
    public long countPeopleSince(LocalDateTime from) {
        Long count = queryFactory
                .select(captchaResult.client.id.countDistinct())
                .from(captchaResult)
                .where(from == null ? null : captchaResult.createdAt.goe(from))
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public CaptchaMyStat findMyStat(Long clientId) {
        Tuple summary = queryFactory
                .select(captchaResult.count(),
                        captchaResult.elapsedSecond.min(),
                        captchaResult.elapsedSecond.avg())
                .from(captchaResult)
                .where(captchaResult.client.id.eq(clientId))
                .fetchOne();

        Float latest = queryFactory
                .select(captchaResult.elapsedSecond)
                .from(captchaResult)
                .where(captchaResult.client.id.eq(clientId))
                .orderBy(captchaResult.createdAt.desc())
                .limit(1)
                .fetchOne();

        if (summary == null) {
            return new CaptchaMyStat(0, 0f, 0f, 0f);
        }

        Long count = summary.get(captchaResult.count());
        Float best = summary.get(captchaResult.elapsedSecond.min());
        Double average = summary.get(captchaResult.elapsedSecond.avg());

        return new CaptchaMyStat(
                count == null ? 0 : count,
                best == null ? 0f : best,
                average == null ? 0f : average.floatValue(),
                latest == null ? 0f : latest);
    }

    @Override
    public List<Float> findRecentElapsedSeconds(Long clientId, int limit) {
        List<Float> recent = new ArrayList<>(queryFactory
                .select(captchaResult.elapsedSecond)
                .from(captchaResult)
                .where(captchaResult.client.id.eq(clientId))
                .orderBy(captchaResult.createdAt.desc())
                .limit(limit)
                .fetch());

        Collections.reverse(recent);
        return recent;
    }
}
