package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<Post> search(PostQueryCondition condition, Pageable pageable);

    /**
     * sitemap 에 올릴 글. 반응이 하나도 없는 짧은 글까지 색인시키면 얇은 콘텐츠로 사이트 평가가 깎인다.
     * 추천은 자기 글에 못 누르므로(Q4) 추천 1개는 남이 남긴 반응이라는 뜻이다.
     */
    java.util.List<Post> findForSitemap(int minContentLength, int limit);
}
