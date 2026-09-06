package com.practicket.ad.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {

    Optional<Advertiser> findByName(String name);

    boolean existsByName(String name);

    List<Advertiser> findAllByOrderByNameAsc();
}
