package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.enums.ArtFilterType;
import com.example.ticketing.art.domain.enums.ArtSortType;
import com.example.ticketing.art.domain.enums.SortDirection;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtQueryCondition {
    private String keyword;
    private ArtSortType sortBy;
    private SortDirection sortDirection;
    private ArtFilterType filterType;
    private Long currentClientId;
}
