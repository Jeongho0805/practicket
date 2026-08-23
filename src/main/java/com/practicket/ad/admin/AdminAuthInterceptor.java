package com.practicket.ad.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /admin-hoya/** 접근을 세션 로그인 여부로 가드한다. 로그인/로그아웃 경로는 예외.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_ATTR = "ADMIN_AUTHED";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.equals("/admin-hoya/login") || uri.equals("/admin-hoya/logout")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(AUTH_ATTR))) {
            return true;
        }
        response.sendRedirect("/admin-hoya/login");
        return false;
    }
}
