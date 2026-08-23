package com.practicket.ad.exception;

/**
 * 광고 어드민(슬롯/배너 CRUD, 이미지 업로드) 관련 예외.
 * 공용 {@code common.exception.GlobalException}/{@code ErrorCode}는 건드리지 않기 위해
 * ad 패키지 전용으로 별도 정의한다.
 */
public class AdException extends RuntimeException {

    public AdException(String message) {
        super(message);
    }
}
