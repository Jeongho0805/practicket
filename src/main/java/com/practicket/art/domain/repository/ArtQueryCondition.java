package com.practicket.art.domain.repository;

import com.practicket.art.domain.enums.ArtFilterType;
import com.practicket.art.domain.enums.ArtSortType;
import com.practicket.art.domain.enums.SortDirection;
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
