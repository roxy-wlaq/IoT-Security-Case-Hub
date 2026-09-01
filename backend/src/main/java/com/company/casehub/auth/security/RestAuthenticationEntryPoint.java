package com.company.casehub.auth.security;

import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.response.ApiErrorResponse;
import com.company.casehub.common.web.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns 401 JSON for unauthenticated requests instead of redirecting to a
 * login page (SPA consumes the API directly).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.AUTH_UNAUTHENTICATED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object traceAttr = request.getAttribute(TraceIdFilter.REQUEST_ATTR);
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.AUTH_UNAUTHENTICATED.name(),
                ErrorCode.AUTH_UNAUTHENTICATED.getDefaultMessage(),
                traceAttr != null ? traceAttr.toString() : null,
                Map.of());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
