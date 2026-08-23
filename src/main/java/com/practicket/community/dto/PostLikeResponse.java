package com.practicket.community.dto;

import lombok.Builder;
import lombok.Getter;

/** 화면이 이 값으로 버튼과 숫자를 갈아끼운다. 새로고침하면 안 된다 — 애드센스 무효 트래픽 */
@Getter
@Builder
public class PostLikeResponse {

    private final Boolean liked;
    private final Long likeCount;
}
