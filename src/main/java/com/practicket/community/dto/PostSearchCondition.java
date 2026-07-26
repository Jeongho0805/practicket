package com.practicket.community.dto;

import lombok.Data;

@Data
public class PostSearchCondition {

    /** 제목 검색어. 정렬은 작성일 최신순 고정이라 정렬 옵션은 받지 않는다(Q4-2). */
    private String keyword;
}
