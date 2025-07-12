package com.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 409 Conflict
    SEAT_ALREADY_BOOKED(409, "T01", "이미 선택된 좌석입니다."),

    TICKETING_TIME_IS_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "T02", "예매가능한 시간이 아닙니다."),

    SESSION_IS_NOT_EXIST(404, "A01", "세션이 존재하지 않습니다."),

    PARAMETER_IS_NOT_VALID(400, "P01", "입력값이 올바르지 않습니다."),

    INTERNAL_SERVER_ERROR(500, "S01", "서버 통신 에러가 발생하였습니다. 잠시 후 다시 이용해주세요."),
    RESOURCE_NOT_FOUND(404, "R01", "존재하지 않는 리소스입니다.")
    ;



    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
