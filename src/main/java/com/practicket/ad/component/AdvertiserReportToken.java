package com.practicket.ad.component;

import com.practicket.ad.exception.AdException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 광고주 통합 리포트 주소(/ad/report/advertiser/{token})의 토큰.
 *
 * 배너 리포트와 달리 저장된 토큰이 없다 — 광고주가 별도 테이블이 아니라 배너의 이름 문자열이기 때문이다.
 * 그래서 이름을 서버 시크릿으로 HMAC 서명해 토큰을 만든다. 저장 없이도 남이 맞힐 수 없고,
 * 같은 이름이면 항상 같은 주소가 나와 광고주가 북마크해 둘 수 있다.
 *
 * 한계(의도한 것):
 * - 광고주명이 바뀌면 주소도 바뀐다 → 새 링크를 다시 줘야 한다.
 * - 특정 광고주 링크만 폐기할 수 없다(시크릿을 바꾸면 전부 무효가 된다).
 * 둘 중 하나가 실제로 문제가 되면 그때 advertiser 테이블로 승격한다.
 */
@Component
public class AdvertiserReportToken {

    private static final String ALGORITHM = "HmacSHA256";
    /** 같은 시크릿을 다른 용도와 나눠 쓰므로 용도 접두사로 서명 공간을 분리한다. */
    private static final String PURPOSE = "ad-advertiser-report:";
    /** 128비트면 추측 공격에 충분하다. 주소가 짧을수록 전달하기 좋다. */
    private static final int TOKEN_BYTES = 16;

    private final byte[] secret;

    public AdvertiserReportToken(@Value("${app.secret.ticket-token}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(String advertiserName) {
        byte[] full = sign(PURPOSE + advertiserName);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Arrays.copyOf(full, TOKEN_BYTES));
    }

    /** 타이밍 공격을 피하려고 문자열 비교 대신 상수 시간 비교를 쓴다. */
    public boolean matches(String advertiserName, String token) {
        return MessageDigest.isEqual(
                issue(advertiserName).getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sign(String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AdException("리포트 토큰을 만들 수 없습니다.");
        }
    }
}
