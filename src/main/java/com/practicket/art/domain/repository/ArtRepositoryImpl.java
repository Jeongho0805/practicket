package com.practicket.art.domain.repository;

import com.practicket.art.domain.entity.Art;
import com.practicket.art.domain.enums.ArtFilterType;
import com.practicket.art.domain.enums.ArtSortType;
import com.practicket.art.domain.enums.SortDirection;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.practicket.art.domain.entity.QArt.art;
import static com.practicket.client.domain.QClient.client;

@Repository
@RequiredArgsConstructor
public class ArtRepositoryImpl implements ArtRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Art> searchArts(ArtQueryCondition condition, Pageable pageable) {
        // POPULAR 필터인 경우 특별 처리
        if (condition.getFilterType() == ArtFilterType.POPULAR) {
            return searchPopularArts(condition, pageable);
        }

        // TODAY_HOT 필터인 경우 특별 처리
        if (condition.getFilterType() == ArtFilterType.HOT) {
            return searchHotArts(condition, pageable);
        }

        List<Art> content = queryFactory
                .selectFrom(art)
                .leftJoin(art.client, client).fetchJoin()
                .where(
                        keywordCondition(condition.getKeyword()),
                        filterCondition(condition.getFilterType(), condition.getCurrentClientId())
                )
                .orderBy(getOrderSpecifiers(condition.getSortBy(), condition.getSortDirection()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(art.count())
                .from(art)
                .where(
                        keywordCondition(condition.getKeyword()),
                        filterCondition(condition.getFilterType(), condition.getCurrentClientId())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private Page<Art> searchPopularArts(ArtQueryCondition condition, Pageable pageable) {
        // 인기 점수 계산: (좋아요 * 3) + (댓글 * 2) + (조회수 * 1)
        // 상위 10개만 반환 (페이지네이션 무시)
        NumberExpression<Integer> popularityScore = art.likeCount.multiply(3)
                .add(art.commentCount.multiply(2))
                .add(art.viewCount);

        List<Art> content = queryFactory
                .selectFrom(art)
                .leftJoin(art.client, client).fetchJoin()
                .where(keywordCondition(condition.getKeyword()))
                .orderBy(popularityScore.desc(), art.createdAt.desc())
                .limit(10)
                .fetch();

        long total = content.size();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    private Page<Art> searchHotArts(ArtQueryCondition condition, Pageable pageable) {
        // 최근 7일 이내 작품 중 인기 있는 작품 상위 10개만 반환 (페이지네이션 무시)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        NumberExpression<Integer> popularityScore = art.likeCount.multiply(3)
                .add(art.commentCount.multiply(2))
                .add(art.viewCount);

        List<Art> content = queryFactory
                .selectFrom(art)
                .leftJoin(art.client, client).fetchJoin()
                .where(
                        keywordCondition(condition.getKeyword()),
                        art.createdAt.after(weekAgo)
                )
                .orderBy(popularityScore.desc(), art.createdAt.desc())
                .limit(10)
                .fetch();

        long total = content.size();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return art.title.containsIgnoreCase(keyword)
                .or(art.client.name.containsIgnoreCase(keyword));
    }

    private BooleanExpression filterCondition(ArtFilterType filterType, Long currentClientId) {
        if (filterType == null) {
            return null;
        }

        if (filterType == ArtFilterType.ONLY_MINE) {
            return currentClientId != null ? art.client.id.eq(currentClientId) : null;
        }

        return null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(ArtSortType sortBy, SortDirection sortDirection) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        boolean isAsc = sortDirection == SortDirection.ASC;

        switch (sortBy) {
            case LATEST -> orderSpecifiers.add(isAsc ? art.createdAt.asc() : art.createdAt.desc());
            case LIKE -> {
                orderSpecifiers.add(isAsc ? art.likeCount.asc() : art.likeCount.desc());
                orderSpecifiers.add(art.createdAt.desc());
            }
            case VIEW -> {
                orderSpecifiers.add(isAsc ? art.viewCount.asc() : art.viewCount.desc());
                orderSpecifiers.add(art.createdAt.desc());
            }
            case COMMENT -> {
                orderSpecifiers.add(isAsc ? art.commentCount.asc() : art.commentCount.desc());
                orderSpecifiers.add(art.createdAt.desc());
            }
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
