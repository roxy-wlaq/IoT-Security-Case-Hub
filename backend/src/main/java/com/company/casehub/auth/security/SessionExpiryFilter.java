package com.company.casehub.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Closes the "expire sessions" loop (Phase 0-3 review, HIGH-01).
 *
 * <p>When an administrator disables a user or resets a password,
 * {@link com.company.casehub.auth.service.SessionRegistryService#expireSessions}
 * marks the affected sessions via {@link SessionInformation#expireNow()}. This filter
 * inspects that flag on every request: if the current HTTP session is expired it
 * invalidates the session and clears the {@code SecurityContext}, so the next request
 * is unauthenticated (401 / forced re-login) instead of the user silently staying
 * logged in.</p>
 */
@RequiredArgsConstructor
public class SessionExpiryFilter extends OncePerRequestFilter {

    private final SessionRegistry sessionRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SessionInformation info = sessionRegistry.getSessionInformation(session.getId());
            if (info != null && info.isExpired()) {
                sessionRegistry.removeSessionInformation(session.getId());
                try {
                    session.invalidate();
                } catch (IllegalStateException ignored) {
                    // Already invalidated by a concurrent request.
                }
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
