package com.practicket.community.domain.repository;

import com.practicket.community.component.TagNormalizer;
import com.practicket.community.domain.entity.Post;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.practicket.community.domain.entity.QPost.post;
import static com.practicket.community.domain.entity.QPostTag.postTag;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> search(PostQueryCondition condition, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .where(
                        post.deletedAt.isNull(),
                        keywordContains(condition.getKeyword()),
                        taggedWith(condition.getTag()),
                        writtenBy(condition.getClientId())
                )
                .orderBy(orderSpecifiers(condition.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.deletedAt.isNull(),
                        keywordContains(condition.getKeyword()),
                        taggedWith(condition.getTag()),
                        writtenBy(condition.getClientId())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /** 제목과 태그만 뒤진다. 본문은 TEXT 라 LIKE '%…%' 가 인덱스를 못 타 전체 스캔이 된다 */
    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        BooleanExpression titleMatch = post.title.containsIgnoreCase(keyword.trim());
        BooleanExpression tagMatch = taggedWith(TagNormalizer.normalizeOne(keyword));

        return tagMatch == null ? titleMatch : titleMatch.or(tagMatch);
    }

    /** id.desc() 타이브레이커가 없으면 값이 같은 글들의 순서가 페이지마다 흔들려 중복 노출된다 */
    private OrderSpecifier<?>[] orderSpecifiers(PostQueryCondition.PostSortType sort) {
        return switch (sort) {
            case LIKE -> new OrderSpecifier[]{post.likeCount.desc(), post.id.desc()};
            case VIEW -> new OrderSpecifier[]{post.viewCount.desc(), post.id.desc()};
            case COMMENT -> new OrderSpecifier[]{post.commentCount.desc(), post.id.desc()};
            case LATEST -> new OrderSpecifier[]{post.createdAt.desc(), post.id.desc()};
        };
    }

    private BooleanExpression writtenBy(Long clientId) {
        return clientId == null ? null : post.client.id.eq(clientId);
    }

    /** 조인이 아니라 exists 로 건다 — 조인하면 태그가 셋인 글이 목록에 세 번 나온다 */
    private BooleanExpression taggedWith(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        return JPAExpressions
                .selectOne()
                .from(postTag)
                .where(postTag.post.eq(post), postTag.tag.eq(tag))
                .exists();
    }
}
