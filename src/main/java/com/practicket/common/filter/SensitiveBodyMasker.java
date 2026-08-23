package com.practicket.common.filter;

import java.util.regex.Pattern;

/**
 * 요청 본문을 로그에 남기기 전에 민감한 값을 가린다.
 *
 * 삭제 비밀번호는 DB 에 BCrypt 로 저장하는데(delete_password_hash) 로그에 원문이 남으면
 * 해싱한 의미가 사라진다. 자물쇠를 채우고 열쇠를 문 앞에 두는 꼴이다.
 * 사람들은 게시판 비밀번호에도 다른 서비스에서 쓰는 값을 넣기 때문에,
 * 지키는 대상은 글이 아니라 그 사람의 비밀번호다.
 *
 * <p>키 이름을 <b>부분 일치</b>로 찾는다. 완전 일치로 하면 {@code deletePassword} 처럼
 * 접두어가 붙은 필드를 놓친다 — 실제로 이 프로젝트의 필드명이 그렇다.
 */
public final class SensitiveBodyMasker {

    private static final String MASK = "\"***\"";

    /**
     * 키에 이 중 하나라도 들어가면 값을 가린다.
     * 새 API 가 생겨도 이름만 관례를 따르면 자동으로 걸린다.
     */
    private static final String SENSITIVE_KEYS = "password|passwd|pwd|email|token|secret|phone";

    /**
     * 1번 그룹은 {@code "키":} 까지, 2번 그룹은 큰따옴표로 감싼 값.
     * 값 안의 이스케이프({@code \"})를 한 글자로 보아야 문자열 끝을 잘못 잡지 않는다.
     */
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(\"[^\"]*(?:" + SENSITIVE_KEYS + ")[^\"]*\"\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\")",
            Pattern.CASE_INSENSITIVE);

    private SensitiveBodyMasker() {
    }

    public static String mask(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        return SENSITIVE_FIELD.matcher(body).replaceAll("$1" + MASK);
    }
}
