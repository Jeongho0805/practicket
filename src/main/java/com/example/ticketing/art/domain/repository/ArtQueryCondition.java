package com.example.ticketing.art.domain.repository;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtQueryCondition {
    private String keyword;              // 제목 검색용
    private String sortBy;               // latest, like, view, comment
    private String sortDirection;        // asc, desc
    private Long currentClientId;        // 내 작품만 보기 필터용
}
