package com.company.casehub.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Guards the frozen CSRF contract: the server must validate the {@code X-XSRF-TOKEN}
 * header the SPA actually sends (not Spring's {@code X-CSRF-TOKEN} default).
 *
 * <p>This is a pure unit test (no Spring context, no DB). It exercises
 * {@link SecurityConfig#csrfTokenRepository()} directly. The {@code CsrfFilter} reads the
 * expected header name from the generated token, so we assert it on the token — if
 * {@code setHeaderName} is ever dropped, every browser mutation would be silently
 * rejected.</p>
 */
class SecurityConfigTest {

    @Test
    void securityConfigWiresFrozenXxsrfHeader() {
        // The @Bean method does not depend on the injected collaborators, so nulls are safe here.
        SecurityConfig config = new SecurityConfig(null, null, null);
        CsrfTokenRepository repository = config.csrfTokenRepository();

        assertThat(repository).isInstanceOf(CookieCsrfTokenRepository.class);

        HttpServletRequest request = new MockHttpServletRequest();
        CsrfToken token = repository.generateToken(request);

        assertThat(token.getHeaderName()).isEqualTo(SecurityConstants.CSRF_HEADER_NAME);
    }
}
