package com.company.casehub.auth.security;

import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.response.ApiErrorResponse;
import com.company.casehub.common.web.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * After authentication, blocks any protected request while the principal still has
 * {@code mustChangePassword=true}, except for the endpoints needed to complete the
 * password change (and CSRF/health). Enforces the "force password change" rule.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHEN_FORCED = Set.of(
            "/api/v1/auth/csrf",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password",
            "/actuator/health");

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword()) {

            String path = request.getRequestURI();
            if (!ALLOWED_WHEN_FORCED.contains(path)) {
                response.setStatus(ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED.getHttpStatus().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Object traceAttr = request.getAttribute(TraceIdFilter.REQUEST_ATTR);
                ApiErrorResponse body = ApiErrorResponse.of(
                        ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED.name(),
                        ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED.getDefaultMessage(),
                        traceAttr != null ? traceAttr.toString() : null,
                        java.util.Map.of());
                response.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
