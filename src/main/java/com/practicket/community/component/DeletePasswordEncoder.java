package com.practicket.community.component;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 글·댓글 삭제 비밀번호(네 자리)를 bcrypt 로 해싱한다.
 *
 * 평문 저장 금지 — MySQL 3306 이 인터넷에 노출돼 있고
 * (docs/server/security-audit-2026-06-03.md), 사람들은 같은 네 자리를 다른 곳에도 쓴다.
 *
 * 인코더 인스턴스를 내부에 직접 들고 있는 이유는 어드민 인증이 쓰는
 * PasswordEncoder 빈(AdminSecurityConfig, fix/home-renewal)과 충돌하지 않기 위해서다.
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
