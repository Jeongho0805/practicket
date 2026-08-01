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

    INQUIRY_RATE_LIMIT_EXCEEDED(429, "IQ01", "문의를 너무 자주 보내고 있습니다. 잠시 후 다시 시도해주세요."),
    INQUIRY_DAILY_LIMIT_EXCEEDED(429, "IQ02", "하루에 보낼 수 있는 문의 수를 초과했습니다."),

    POST_NOT_FOUND(404, "PO01", "존재하지 않는 글입니다."),
    POST_FORBIDDEN(403, "PO02", "본인이 작성한 글이 아닙니다."),
    POST_PASSWORD_REQUIRED(400, "PO03", "삭제 비밀번호를 입력해주세요."),
    POST_PASSWORD_MISMATCH(400, "PO04", "삭제 비밀번호가 일치하지 않습니다."),
    POST_PASSWORD_LOCKED(429, "PO05", "비밀번호를 여러 번 틀렸습니다. 잠시 후 다시 시도해주세요."),
    // 자기 글 추천을 막지 않으면 스팸글을 쓰고 본인이 눌러 sitemap 색인 조건을 통과시킬 수 있다(5-1항).
    POST_LIKE_SELF(400, "PO06", "본인이 쓴 글은 추천할 수 없습니다."),
    POST_PASSWORD_TOO_COMMON(400, "PO07", "너무 흔한 비밀번호입니다. 다른 네 자리를 써주세요."),
    // 토큰 기준. 정상 사용자는 60초에 두 번 글을 쓰지 않으므로 걸리지 않는다(Q6).
    POST_RATE_LIMIT_TOKEN(429, "PO08", "글은 60초에 한 번만 쓸 수 있습니다. 잠시 후 다시 시도해주세요."),
    // IP 기준. 캐리어 NAT 피해를 줄이려고 시간당 10개로 느슨하게 잡는다(Q6).
    POST_RATE_LIMIT_IP(429, "PO09", "같은 네트워크에서 글을 너무 많이 썼습니다. 1시간 후 다시 시도해주세요."),
    POST_BANNED(403, "PO10", "이용이 제한되어 글을 쓸 수 없습니다. 제한 기간이 끝나면 다시 쓸 수 있습니다."),

    COMMENT_NOT_FOUND(404, "PC01", "존재하지 않는 댓글입니다."),
    COMMENT_FORBIDDEN(403, "PC02", "본인이 작성한 댓글이 아닙니다."),
    COMMENT_RATE_LIMIT_TOKEN(429, "PC03", "댓글은 10초에 한 번만 달 수 있습니다. 잠시 후 다시 시도해주세요."),
    COMMENT_RATE_LIMIT_IP(429, "PC04", "같은 네트워크에서 댓글을 너무 많이 달았습니다. 1시간 후 다시 시도해주세요."),
    COMMENT_BANNED(403, "PC05", "이용이 제한되어 댓글을 달 수 없습니다. 제한 기간이 끝나면 다시 달 수 있습니다."),

    // 신고 / 자동 블라인드(Q7). 중복 방지 기준은 IP — 토큰은 무제한 재발급이 가능해
    // 토큰 기준이면 한 사람이 멀쩡한 글·댓글을 혼자 가려버릴 수 있다.
    REPORT_ALREADY_REPORTED(400, "RP01", "이미 신고한 내용입니다."),
    // IP 당 하루 10건. 신고 자체가 어뷰징 수단이 되는 것을 막는다.
    REPORT_RATE_LIMIT_IP(429, "RP02", "신고를 너무 많이 보냈습니다. 잠시 후 다시 시도해주세요."),

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
