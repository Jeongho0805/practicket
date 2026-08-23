package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    List<PostTag> findByPostOrderByIdAsc(Post post);

    /** 글마다 따로 조회하면 20행 목록에 쿼리가 21번 나간다 */
    List<PostTag> findByPostIdInOrderByIdAsc(Collection<Long> postIds);

    /* 파생 삭제는 지울 대상을 예약만 해서 DELETE 가 커밋까지 밀린다.
       그 사이 IDENTITY 키인 새 태그가 먼저 INSERT 되어 유니크 제약에 걸린다.
       벌크 DELETE 는 호출 시점에 실행되므로 순서가 뒤집히지 않는다. */
    @Modifying(flushAutomatically = true)
    @Query("delete from PostTag t where t.post = :post")
    void deleteByPost(@Param("post") Post post);

    /** 누적이 아니라 최근 30일 기준 — 끝난 콘서트 태그가 영영 1등에 박히면 안 된다 */
    @Query("""
            SELECT t.tag AS tag, COUNT(t) AS useCount
            FROM PostTag t JOIN t.post p
            WHERE t.createdAt >= :since AND p.deletedAt IS NULL
            GROUP BY t.tag
            HAVING COUNT(t) >= :minUseCount
            ORDER BY COUNT(t) DESC, MAX(t.createdAt) DESC
            """)
    List<TagUseCount> findPopularTags(@Param("since") LocalDateTime since,
                                      @Param("minUseCount") long minUseCount);

    /** 집계 결과 한 줄 */
    interface TagUseCount {
        String getTag();

        Long getUseCount();
    }
}
