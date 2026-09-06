package com.practicket.practice.infra.persistence;

import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeBestResult;
import com.practicket.practice.domain.PracticeType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.practicket.practice.domain.QPracticeBestResult.practiceBestResult;

@RequiredArgsConstructor
public class PracticeBestResultRepositoryImpl implements PracticeBestResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PracticeBestResult> findRanking(PracticeType type, PeriodType period,
                                                Integer cursorTotalDurationMs, Long cursorResultId,
                                                int limit) {
        return queryFactory
                .selectFrom(practiceBestResult)
                .where(inBucket(type, period), afterCursor(cursorTotalDurationMs, cursorResultId))
                .orderBy(practiceBestResult.totalDurationMs.asc(), practiceBestResult.resultId.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<PracticeBestResult> findMyBest(PracticeType type, PeriodType period, String clientKey) {
        return Optional.ofNullable(queryFactory
                .selectFrom(practiceBestResult)
                .where(inBucket(type, period), practiceBestResult.clientKey.eq(clientKey))
                .fetchOne());
    }

    @Override
    public long countParticipants(PracticeType type, PeriodType period) {
        return count(inBucket(type, period));
    }

    @Override
    public long countFasterThan(PracticeType type, PeriodType period, int totalDurationMs) {
        return count(inBucket(type, period), practiceBestResult.totalDurationMs.lt(totalDurationMs));
    }

    @Override
    public Optional<Integer> findMsAtOffset(PracticeType type, PeriodType period,
                                            long offset, boolean fromSlowest) {
        return Optional.ofNullable(queryFactory
                .select(practiceBestResult.totalDurationMs)
                .from(practiceBestResult)
                .where(inBucket(type, period))
                .orderBy(fromSlowest
                        ? practiceBestResult.totalDurationMs.desc()
                        : practiceBestResult.totalDurationMs.asc())
                .offset(offset)
                .limit(1)
                .fetchOne());
    }

    @Override
    public List<HistogramBin> findHistogram(PracticeType type, PeriodType period, int binWidthMs) {
        NumberExpression<Integer> bin = Expressions.numberTemplate(Integer.class,
                "floor({0} / {1})", practiceBestResult.totalDurationMs, binWidthMs);

        return queryFactory
                .select(bin, practiceBestResult.count())
                .from(practiceBestResult)
                .where(inBucket(type, period))
                .groupBy(bin)
                .orderBy(bin.asc())
                .fetch()
                .stream()
                .map(row -> new HistogramBin(
                        row.get(0, Integer.class) * binWidthMs,
                        row.get(1, Long.class).intValue()))
                .toList();
    }

    private long count(BooleanExpression... conditions) {
        Long count = queryFactory
                .select(practiceBestResult.count())
                .from(practiceBestResult)
                .where(conditions)
                .fetchOne();
        return count == null ? 0 : count;
    }

    private BooleanExpression inBucket(PracticeType type, PeriodType period) {
        return practiceBestResult.type.eq(type)
                .and(practiceBestResult.periodType.eq(period))
                .and(practiceBestResult.periodStart.eq(period.currentBucketStart()));
    }

    /** 기록만 비교하면 동점 구간에서 같은 줄이 다시 나오거나 건너뛴다. 원본 id 를 함께 본다. */
    private BooleanExpression afterCursor(Integer cursorTotalDurationMs, Long cursorResultId) {
        if (cursorTotalDurationMs == null) {
            return null;
        }
        return practiceBestResult.totalDurationMs.gt(cursorTotalDurationMs)
                .or(practiceBestResult.totalDurationMs.eq(cursorTotalDurationMs)
                        .and(practiceBestResult.resultId.gt(cursorResultId)));
    }
}
