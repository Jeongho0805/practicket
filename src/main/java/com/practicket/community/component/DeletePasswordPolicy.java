package com.practicket.community.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;

import java.util.Set;

/** 공격자는 1만 개를 다 넣어보지 않는다. 사람들이 실제로 쓰는 몇 개만 넣어본다 */
public final class DeletePasswordPolicy {

    /** 네 자리 비밀번호 통계에서 압도적으로 자주 나오는 값 */
    private static final Set<String> BANNED = Set.of(
            "0000", "1111", "2222", "3333", "4444",
            "5555", "6666", "7777", "8888", "9999",
            "1234", "4321", "1212", "2121", "1004",
            "0123", "2580", "1122", "1313", "6969"
    );

    private DeletePasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword != null && BANNED.contains(rawPassword)) {
            throw new ValidateException(ErrorCode.POST_PASSWORD_TOO_COMMON);
        }
    }
}
