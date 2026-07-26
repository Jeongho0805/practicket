package com.practicket.community.domain.repository;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostQueryCondition {

    /**
     * 제목 검색어. 본문은 검색하지 않는다 —
     * TEXT 컬럼에 LIKE '%…%' 를 걸면 인덱스를 못 타 전체 스캔이 된다(Q9).
     * 태그 검색은 post_tag 가 생기는 5단계에서 붙인다.
     */
    private final String keyword;
}
