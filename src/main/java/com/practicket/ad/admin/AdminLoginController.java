package com.practicket.ad.admin;

import com.practicket.ad.exception.AdException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 어드민 로그인/로그아웃. 성공 시 세션에 인증 플래그를 심는다.
 */
@Controller
@RequiredArgsConstructor
public class AdminLoginController {

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
                request.getSession(true).setAttribute(AdminAuthInterceptor.AUTH_ATTR, Boolean.TRUE);
                return "redirect:/admin-hoya/ad/banners";
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
