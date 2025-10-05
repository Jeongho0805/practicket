package com.example.ticketing.art.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ArtQueryCondition {
    private String keyword;              // 제목 검색용
    private List<Long> matchedClientIds; // 닉네임 검색으로 매칭된 clientId들
    private String sortBy;               // latest, like, view, comment
    private String sortDirection;        // asc, desc
    private Long currentClientId;        // 내 작품만 보기 필터용
}
