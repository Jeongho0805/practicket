package com.example.ticketing.common.auth;

import com.example.ticketing.auth.dto.SessionObject;
import com.example.ticketing.common.exception.AuthException;
import com.example.ticketing.common.Constant;
import com.example.ticketing.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
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
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        Object object = session.getAttribute(Constant.SESSION_KEY);
        if (!(object instanceof SessionObject)) {
            throw new AuthException(ErrorCode.SESSION_IS_NOT_EXIST);
        }
        SessionObject sessionObject = (SessionObject) object;
        return new UserInfo(sessionObject.getIp(), sessionObject.getName(), session.getId());
    }
}