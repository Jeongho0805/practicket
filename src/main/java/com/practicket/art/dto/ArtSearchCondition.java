package com.practicket.art.dto;

import com.practicket.art.domain.enums.ArtFilterType;
import com.practicket.art.domain.enums.ArtSortType;
import com.practicket.art.domain.enums.SortDirection;
import lombok.Data;

@Data
public class ArtSearchCondition {
    private String keyword;
    private ArtSortType sortBy = ArtSortType.LATEST;
    private SortDirection sortDirection = SortDirection.DESC;
    private ArtFilterType filterType;
}
