package com.practicket.community.dto;

/** 아직 안 쓰인 씨앗 태그는 사용 수가 0 이고, 화면이 그 숫자를 감춘다 */
public record PopularTagResponse(String tag, long useCount) {

    public boolean hasUseCount() {
        return useCount > 0;
    }
}
