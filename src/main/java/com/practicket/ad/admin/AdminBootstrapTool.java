package com.practicket.ad.admin;

import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 어드민 계정 부트스트랩 — 최초 1회 로컬에서만 실행하는 독립 유틸(Spring 빈 아님).
 *
 * 비밀번호를 입력하면 bcrypt 해시 + TOTP 시크릿 + otpauth URI를 출력한다.
 * 출력된 해시·시크릿을 admin_account 테이블에 직접 insert하고,
 * otpauth URI를 QR로 변환해 OTP 앱(Google Authenticator 등)에 등록한다.
 * 평문 비밀번호는 코드·git·DB 어디에도 남지 않는다.
 *
 * 실행: IDE에서 main 실행, 또는 인자로 비번 전달
 *   java -cp <classpath> com.practicket.ad.admin.AdminBootstrapTool [username] [password]
 */
public class AdminBootstrapTool {

    public static void main(String[] args) throws Exception {
        String username = args.length > 0 ? args[0] : "admin";
        String password = args.length > 1 ? args[1] : prompt("어드민 비밀번호를 입력하세요: ");

        String passwordHash = new BCryptPasswordEncoder().encode(password);
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        QrData qrData = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer("practicket-admin")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        System.out.println();
        System.out.println("===== 어드민 계정 (admin_account 테이블에 insert) =====");
        System.out.println("username     : " + username);
        System.out.println("password_hash: " + passwordHash);
        System.out.println("totp_secret  : " + secret);
        System.out.println();
        System.out.println("----- OTP 앱 등록: 아래 URI를 QR로 만들거나, 시크릿을 앱에 수동 입력 -----");
        System.out.println(qrData.getUri());
        System.out.println();
        System.out.println("INSERT 예시:");
        System.out.println("INSERT INTO admin_account(username, password_hash, totp_secret) VALUES ('"
                + username + "', '" + passwordHash + "', '" + secret + "');");
    }

    private static String prompt(String message) throws Exception {
        System.out.print(message);
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}
