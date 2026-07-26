package com.practicket.community.dto;

import lombok.Data;

/**
 * 토큰이 작성자와 일치하면 이 요청 자체가 필요 없다.
 * 기기를 바꿔 토큰이 다를 때만 비밀번호를 받는다(Q5).
 *
 * 비밀번호를 쿼리 파라미터가 아니라 본문으로 받는 이유는
 * URL 이 접근 로그·리퍼러에 그대로 남기 때문이다.
 */
@Data
public class PostDeleteRequest {

    private String deletePassword;
}
