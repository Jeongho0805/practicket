package com.practicket.community.dto;

import lombok.Data;

@Data
public class PostSearchCondition {

    private String keyword;

    /** 주소로 공유·색인되도록 URL 파라미터로 받는다 (`/community?tag=세븐틴`) */
    private String tag;

    /** 문자열 그대로 받아둔다 — 알 수 없는 값의 기본값 변환은 쓰는 쪽(PostService)이 한다 */
    private String sort;
}
