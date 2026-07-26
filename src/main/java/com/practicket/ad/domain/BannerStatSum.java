package com.practicket.ad.domain;

/**
 * banner_stat_daily를 배너 단위로 합산한 결과(인터페이스 프로젝션).
 * JPQL의 별칭(alias)이 getter 이름과 일치해야 매핑된다.
 */
public interface BannerStatSum {

    Long getBannerId();

    Long getImpressions();

    Long getClicks();
}
