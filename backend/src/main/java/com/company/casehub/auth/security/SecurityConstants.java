package com.company.casehub.auth.security;

/**
 * Frozen CSRF contract (Security & RBAC Detail V1.0 §11-§15).
 *
 * <p>The SPA reads the token from the {@code XSRF-TOKEN} cookie (HttpOnly = false)
 * and echoes it back in the {@code X-XSRF-TOKEN} request header. The server-side
 * HTTP session (JSESSIONID) is the only credential store — no JWT, no LocalStorage.</p>
 *
 * <p>These names are shared between {@link SecurityConfig} (which configures the
 * {@code CookieCsrfTokenRepository}) and {@link com.company.casehub.auth.controller.AuthController}
 * (which reports them to the client via {@code GET /api/v1/auth/csrf}).</p>
 */
public final class SecurityConstants {

    /** Cookie that carries the CSRF token to the browser (HttpOnly = false). */
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    /** Request header the SPA must send the token back in. */
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private SecurityConstants() {
    }
}
