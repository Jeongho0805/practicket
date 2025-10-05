package com.example.ticketing.art.domain;

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

import static com.example.ticketing.art.domain.QArt.art;
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
                        clientIdsCondition(condition.getMatchedClientIds()),
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
                        clientIdsCondition(condition.getMatchedClientIds()),
                        onlyMineCondition(condition.getCurrentClientId())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordCondition(String keyword) {
        return keyword != null ? art.title.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression clientIdsCondition(List<Long> clientIds) {
        return clientIds != null && !clientIds.isEmpty() ? art.client.id.in(clientIds) : null;
    }

    private BooleanExpression onlyMineCondition(Long currentClientId) {
        return currentClientId != null ? art.client.id.eq(currentClientId) : null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(String sortBy, String sortDirection) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        boolean isAsc = "asc".equalsIgnoreCase(sortDirection);

        if (sortBy == null || "latest".equals(sortBy)) {
            orderSpecifiers.add(isAsc ? art.createdAt.asc() : art.createdAt.desc());
        } else if ("like".equals(sortBy)) {
            orderSpecifiers.add(isAsc ? art.likeCount.asc() : art.likeCount.desc());
            orderSpecifiers.add(art.createdAt.desc());
        } else if ("view".equals(sortBy)) {
            orderSpecifiers.add(isAsc ? art.viewCount.asc() : art.viewCount.desc());
            orderSpecifiers.add(art.createdAt.desc());
        } else if ("comment".equals(sortBy)) {
            orderSpecifiers.add(isAsc ? art.comments.size().asc() : art.comments.size().desc());
            orderSpecifiers.add(art.createdAt.desc());
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
