package com.example.ticketing.art.dto;

import com.example.ticketing.art.domain.enums.ArtSortType;
import com.example.ticketing.art.domain.enums.SortDirection;
import lombok.Data;

@Data
public class ArtSearchCondition {
    private String keyword;
    private ArtSortType sortBy = ArtSortType.LATEST;
    private SortDirection sortDirection = SortDirection.DESC;
    private Boolean onlyMine;
}
