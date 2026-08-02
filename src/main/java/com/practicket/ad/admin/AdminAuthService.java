package com.practicket.ad.admin;

import com.practicket.ad.domain.AdminAccount;
import com.practicket.ad.domain.AdminAccountRepository;
import com.practicket.ad.exception.AdException;
import dev.samstevens.totp.code.CodeVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 어드민 로그인 검증: 비밀번호(bcrypt) + TOTP 6자리 둘 다 일치해야 통과. IP 잠금 적용.
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeVerifier totpCodeVerifier;
    private final AdminLoginLockout lockout;

    public boolean login(String username, String password, String code, String ip) {
        if (lockout.isLocked(ip)) {
            throw new AdException("로그인 시도가 많습니다. 15분 후 다시 시도해주세요.");
        }
        Optional<AdminAccount> account = adminAccountRepository.findByUsername(username);
        boolean ok = account.isPresent()
                && passwordEncoder.matches(password, account.get().getPasswordHash())
                && code != null && !code.isBlank()
                && totpCodeVerifier.isValidCode(account.get().getTotpSecret(), code.trim());
        if (!ok) {
            lockout.recordFailure(ip);
            return false;
        }
        lockout.reset(ip);
        return true;
    }
}
