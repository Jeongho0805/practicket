package com.practicket.ad.admin;

import com.practicket.ad.exception.AdException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;

/**
 * 어드민 로그인/로그아웃. 성공 시 세션에 인증 플래그를 심는다.
 */
@Controller
@RequiredArgsConstructor
public class AdminLoginController {

    private static final Duration SESSION_TTL = Duration.ofDays(30);

    private final AdminAuthService adminAuthService;

    @GetMapping("/admin-hoya/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/admin-hoya/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String code,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        String ip = extractIp(request);
        try {
            if (adminAuthService.login(username, password, code, ip)) {
                HttpSession session = request.getSession(true);
                session.setAttribute(AdminAuthInterceptor.AUTH_ATTR, Boolean.TRUE);
                // 혼자 쓰는 운영 콘솔이라 재로그인 비용이 보안 이득보다 크다는 판단은 그대로다.
                // 다만 세션이 Redis 로 가면서 무한(-1)은 TTL 이 안 걸려 키가 영영 남으므로 30일로 끊는다.
                // application.yml이 아니라 여기서 거는 이유: 세션 정책을 앱 전체가 아니라 어드민 세션에만 한정하기 위함.
                session.setMaxInactiveInterval((int) SESSION_TTL.toSeconds());
                return "redirect:/admin-hoya/ad";
            }
            redirectAttributes.addFlashAttribute("error", "로그인 정보가 올바르지 않습니다.");
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-hoya/login";
    }

    @GetMapping("/admin-hoya/logout")
    public String logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return "redirect:/admin-hoya/login";
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
