package com.example.ticketing.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class CustomRequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        if (!httpRequest.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        String threadKey = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            MDC.put("thread-key", threadKey);
            logBasicRequest(wrappedRequest);
            chain.doFilter(wrappedRequest, response);
            logRequestBody(wrappedRequest);
        } finally {
            MDC.clear();
        }
    }
    
    private void logBasicRequest(ContentCachingRequestWrapper request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String clientIp = getClientIp(request);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[REQUEST] ");
        logMessage.append(method).append(" ").append(uri);
        
        if (queryString != null) {
            logMessage.append("?").append(queryString);
        }
        logMessage.append(" | IP: ").append(clientIp);
        log.info(logMessage.toString());
    }
    
    private void logRequestBody(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String body = getRequestBody(request);
            if (body != null && !body.isEmpty()) {
                log.info("[REQUEST BODY] {}", body);
            }
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return body.length() > 1000 ? body.substring(0, 1000) + "..." : body;
        }
        return null;
    }
}