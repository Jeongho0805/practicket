package com.example.ticketing.art.domain.repository;

import com.example.ticketing.art.domain.entity.Art;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtRepositoryCustom {
    Page<Art> searchArts(ArtQueryCondition condition, Pageable pageable);
}
