package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<Post> search(PostQueryCondition condition, Pageable pageable);
}
