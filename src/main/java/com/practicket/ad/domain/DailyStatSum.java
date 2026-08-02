package com.practicket.ad.domain;

import java.time.LocalDate;

/**
 * banner_stat_daily를 날짜 단위로(전 배너 합산) 집계한 결과(인터페이스 프로젝션).
 */
public interface DailyStatSum {

    LocalDate getStatDate();

    Long getImpressions();

    Long getClicks();
}
