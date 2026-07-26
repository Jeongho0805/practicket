package com.practicket.notice.domain;

/**
 * 목록 한 줄. 월 구분 헤더를 붙일지 여부를 서버가 미리 계산해 담는다.
 *
 * 무한스크롤은 앞 배치의 마지막 달을 클라이언트가 다시 보내주므로,
 * 배치가 나뉘어도 같은 달에 헤더가 두 번 찍히지 않는다.
 * {@code monthHeader} 가 null 이면 헤더를 그리지 않는다.
 */
public record NoticeRow(Notice notice, String monthHeader, String monthKey) {
}
