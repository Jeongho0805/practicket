package com.example.ticketing.art.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtRepositoryCustom {
    Page<Art> searchArts(ArtQueryCondition condition, Pageable pageable);
}
