package com.practicket.community.dto;

import lombok.Data;

/** 기기를 바꿔 토큰이 다를 때만 쓴다. 본문으로 받는 이유는 URL 이 접근 로그·리퍼러에 남기 때문이다 */
@Data
public class PostDeleteRequest {

    private String deletePassword;
}
