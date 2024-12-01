package com.example.ticketing.auth.component;

import com.example.ticketing.auth.dto.SessionObject;
import com.example.ticketing.common.AuthException;
import com.example.ticketing.common.Constant;
import com.example.ticketing.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    public SessionObject getSessionObject(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        Object sessionObject = session.getAttribute(Constant.SESSION_KEY);
        if (!(sessionObject instanceof SessionObject)) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        return (SessionObject) sessionObject;
    }

    public void createSession(HttpServletRequest request, String clientIp, String name) {
        HttpSession session = request.getSession();
        session.setAttribute(Constant.SESSION_KEY, new SessionObject(clientIp, name));
    }

    public static void deleteSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        session.invalidate();
    }
}
