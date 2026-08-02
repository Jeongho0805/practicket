package com.practicket.community.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 자유 입력을 받지 않는다 — 어드민이 분류할 수 없고, 그 칸이 또 하나의 욕설 입력창이 된다 */
@Getter
@RequiredArgsConstructor
public enum ReportReason {

    AD("광고/도배"),
    ABUSE("욕설/비방"),
    PRIVACY("개인정보 노출"),
    ETC("기타");

    private final String label;
}
