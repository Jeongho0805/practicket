package com.practicket.art.domain.repository;

import com.practicket.art.domain.entity.Art;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtRepositoryCustom {
    Page<Art> searchArts(ArtQueryCondition condition, Pageable pageable);
}
