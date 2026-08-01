package com.practicket.community.component;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 삭제 비밀번호를 bcrypt 로 해싱한다.
 * 인코더를 빈으로 주입받지 않고 직접 들고 있는 이유는 어드민 인증의 PasswordEncoder 빈과 충돌하지 않기 위해서다.
 */
@Component
public class DeletePasswordEncoder {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
