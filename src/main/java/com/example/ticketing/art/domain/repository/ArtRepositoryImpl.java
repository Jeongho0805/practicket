package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.entity.Art;
import com.example.ticketing.art.domain.enums.ArtSortType;
import com.example.ticketing.art.domain.enums.SortDirection;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.example.ticketing.art.domain.entity.QArt.art;
import static com.example.ticketing.client.domain.QClient.client;

@Repository
@RequiredArgsConstructor
public class ArtRepositoryImpl implements ArtRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Art> searchArts(ArtQueryCondition condition, Pageable pageable) {
        List<Art> content = queryFactory
                .selectFrom(art)
                .leftJoin(art.client, client).fetchJoin()
                .where(
                        keywordCondition(condition.getKeyword()),
                        onlyMineCondition(condition.getCurrentClientId())
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
                        onlyMineCondition(condition.getCurrentClientId())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return art.title.containsIgnoreCase(keyword)
                .or(art.client.name.containsIgnoreCase(keyword));
    }

    private BooleanExpression onlyMineCondition(Long currentClientId) {
        return currentClientId != null ? art.client.id.eq(currentClientId) : null;
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
