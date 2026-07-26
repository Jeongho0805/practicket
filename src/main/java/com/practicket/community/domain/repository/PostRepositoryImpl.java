package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.practicket.community.domain.entity.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 목록은 작성일 최신순 고정이다. 수정일로 정렬하면 자기 글을 계속 수정해
     * 맨 위에 박아두는 끌어올리기 어뷰징을 막을 수 없다(Q4-2).
     *
     * 지운 글 제외는 Post 의 @SQLRestriction 이 이미 처리하지만,
     * 여기서도 명시해 쿼리만 읽어도 의도가 드러나게 둔다.
     */
    @Override
    public Page<Post> search(PostQueryCondition condition, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .where(
                        post.deletedAt.isNull(),
                        keywordContains(condition.getKeyword())
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.deletedAt.isNull(),
                        keywordContains(condition.getKeyword())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return post.title.containsIgnoreCase(keyword.trim());
    }
}
