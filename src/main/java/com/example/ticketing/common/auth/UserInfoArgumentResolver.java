package com.example.ticketing.common.auth;

import com.example.ticketing.auth.dto.SessionObject;
import com.example.ticketing.common.AuthException;
import com.example.ticketing.common.Constant;
import com.example.ticketing.common.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserInfo.class)
                && parameter.hasParameterAnnotation(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Cookie[] cookies = request.getCookies();
        String sessionValue = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Constant.SESSION_KEY.equals(cookie.getName())) {
                    sessionValue = cookie.getValue();
                }
            }
        }
        if (sessionValue == null) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        Object object = session.getAttribute(Constant.SESSION_KEY);
        if (!(object instanceof SessionObject)) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        SessionObject sessionObject = (SessionObject) object;
        return new UserInfo(sessionObject.getIp(), sessionObject.getName(), sessionValue);
    }
}