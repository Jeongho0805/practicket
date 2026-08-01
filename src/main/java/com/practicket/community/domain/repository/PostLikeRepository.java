package com.practicket.community.domain.repository;

import com.practicket.community.domain.entity.Post;
import com.practicket.community.domain.entity.PostLike;
import com.practicket.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostAndClient(Post post, Client client);

    void deleteByPostAndClient(Post post, Client client);
}
