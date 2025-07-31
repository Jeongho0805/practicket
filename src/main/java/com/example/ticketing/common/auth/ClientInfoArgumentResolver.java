package com.example.ticketing.common.auth;

import com.example.ticketing.client.domain.Client;
import com.example.ticketing.client.domain.ClientRepository;
import com.example.ticketing.common.exception.AuthException;
import com.example.ticketing.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@RequiredArgsConstructor
public class ClientInfoArgumentResolver implements HandlerMethodArgumentResolver {

    private final ClientRepository clientRepository;

    private static final String HEADER_KEY = "Authorization";

    private static final String TOKEN_TYPE = "Bearer ";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ClientInfo.class)
                && parameter.hasParameterAnnotation(Auth.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("request is not exist");
        }
        String token = null;
        String header = request.getHeader(HEADER_KEY);

        if (header != null && header.startsWith(TOKEN_TYPE)) {
            token = header.substring(TOKEN_TYPE.length());
        } else {
            token = request.getParameter("token");
        }

        if (token == null || token.isEmpty()) {
            throw new AuthException(ErrorCode.TOKEN_IS_NOT_EXIST);
        }

        Client client = clientRepository.findByToken(token)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));

        return ClientInfo.builder()
                .clientId(client.getId())
                .token(client.getToken())
                .name(client.getName())
                .banned(client.getBanned())
                .banReason(client.getBanReason())
                .build();
    }
}