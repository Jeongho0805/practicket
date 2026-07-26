package com.practicket.common.exception;

public enum ErrorCode {
    // 409 Conflict
    SEAT_ALREADY_BOOKED(409, "T01", "이미 선택된 좌석입니다."),
    TICKETING_TIME_IS_NOT_ALLOWED(400, "T02", "예매가능한 시간이 아닙니다."),
    TICKET_SOLD_OUT(409, "T03", "티켓 재고가 소진되었습니다."),
    ALREADY_EXIST_WAITING_QUEUE(409, "T04", "이미 대기열에 존재합니다."),
    TICKET_TOKEN_IS_NOT_VALID(400, "T05", "옳바르지 않은 접근입니다."),


    TOKEN_IS_NOT_EXIST(404, "C01", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(404, "C02", "토큰값이 유효하지 않습니다."),
    NOT_FOUND_CLIENT(404, "C03", "존재하지 않는 사용자입니다."),

    PARAMETER_IS_NOT_VALID(400, "P01", "입력값이 올바르지 않습니다."),

    INTERNAL_SERVER_ERROR(500, "S01", "서버 통신 에러가 발생하였습니다. 잠시 후 다시 이용해주세요."),
    RESOURCE_NOT_FOUND(404, "R01", "존재하지 않는 리소스입니다."),

    INAPPROPRIATE_CONTENT(400, "V01", "부적절한 내용이 포함되어 있습니다."),

    CHAT_RATE_LIMIT_EXCEEDED(429, "CH01", "메시지를 너무 빠르게 전송하고 있습니다."),
    CHAT_BANNED_USER(403, "CH02", "채팅 전송이 불가합니다."),
    CHAT_TEMP_BANNED(403, "CH03", "도배로 인해 채팅이 10분 정지되었습니다."),

    FORBIDDEN(403, "G01", "접근 권한이 없습니다."),

    PRACTICE_SESSION_NOT_FOUND(404, "PR01", "연습 세션이 존재하지 않거나 만료되었습니다."),
    PRACTICE_SESSION_OWNER_MISMATCH(403, "PR02", "본인의 연습 세션이 아닙니다."),
    PRACTICE_TOO_FAST(400, "PR03", "비정상적으로 빠른 요청입니다."),
    PRACTICE_INVALID_TIMING(400, "PR04", "올바르지 않은 타이밍 데이터입니다."),
    NICKNAME_REQUIRED(400, "PR05", "닉네임을 설정한 후 이용해주세요."),

    POST_NOT_FOUND(404, "PO01", "존재하지 않는 글입니다."),
    POST_FORBIDDEN(403, "PO02", "본인이 작성한 글이 아닙니다."),
    POST_PASSWORD_REQUIRED(400, "PO03", "삭제 비밀번호를 입력해주세요."),
    POST_PASSWORD_MISMATCH(400, "PO04", "삭제 비밀번호가 일치하지 않습니다."),
    POST_PASSWORD_LOCKED(429, "PO05", "비밀번호를 여러 번 틀렸습니다. 10분 후 다시 시도해주세요."),

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
