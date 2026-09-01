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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Returns 403 JSON for authenticated-but-forbidden requests.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.AUTH_UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object traceAttr = request.getAttribute(TraceIdFilter.REQUEST_ATTR);
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.AUTH_UNAUTHORIZED.name(),
                ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage(),
                traceAttr != null ? traceAttr.toString() : null,
                Map.of());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
