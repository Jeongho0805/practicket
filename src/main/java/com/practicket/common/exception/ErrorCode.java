package com.practicket.common.exception;

public enum ErrorCode {
    // 409 Conflict
    SEAT_ALREADY_BOOKED(409, "T01", "이미 선택된 좌석입니다."),
    TICKETING_TIME_IS_NOT_ALLOWED(400, "T02", "예매가능한 시간이 아닙니다."),
    TICKET_SOLD_OUT(409, "T03", "티켓 재고가 소진되었습니다."),
    ALREADY_EXIST_WAITING_QUEUE(409, "T04", "이미 대기열에 존재합니다."),


    TOKEN_IS_NOT_EXIST(404, "C01", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(404, "C02", "토큰값이 유효하지 않습니다."),
    NOT_FOUND_CLIENT(404, "C03", "존재하지 않는 사용자입니다."),

    PARAMETER_IS_NOT_VALID(400, "P01", "입력값이 올바르지 않습니다."),

    INTERNAL_SERVER_ERROR(500, "S01", "서버 통신 에러가 발생하였습니다. 잠시 후 다시 이용해주세요."),
    RESOURCE_NOT_FOUND(404, "R01", "존재하지 않는 리소스입니다."),

    INAPPROPRIATE_CONTENT(400, "V01", "부적절한 내용이 포함되어 있습니다."),

    FORBIDDEN(403, "G01", "접근 권한이 없습니다."),

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
