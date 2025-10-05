package com.example.ticketing.art.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtSearchCondition {
    private String keyword;           // 제목 또는 작성자명 검색
    private String sortBy;            // latest, like, view, comment
    private String sortDirection;     // asc, desc (기본값 desc)
    private Boolean onlyMine;         // 내 작품만 보기
}
