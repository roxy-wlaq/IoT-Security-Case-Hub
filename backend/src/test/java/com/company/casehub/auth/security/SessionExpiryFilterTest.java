package com.company.casehub.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.casehub.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Unit test for {@link SessionExpiryFilter} (no Spring context, no DB, no Testcontainers).
 *
 * <p>Verifies that an expired session is rejected with the SAME unified JSON error body
 * every other unauthenticated request uses (code {@code AUTH_UNAUTHENTICATED}, 401,
 * traceId, details) rather than a raw {@code response.sendError}. The integration test
 * {@code SessionExpirationIT} asserts the same {@code $.code} contract end-to-end.</p>
 */
class SessionExpiryFilterTest {

    // Mirror Spring Boot's default ObjectMapper (JavaTimeModule + ISO-8601 timestamps) so the
    // real RestAuthenticationEntryPoint can serialize ApiErrorResponse.timestamp (Instant).
    private static final ObjectMapper ENTRY_POINT_MAPPER = new ObjectMapper();
    static {
        ENTRY_POINT_MAPPER.registerModule(new JavaTimeModule());
        ENTRY_POINT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private final SessionRegistry registry = mock(SessionRegistry.class);
    private final AuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(ENTRY_POINT_MAPPER);
    private final SessionExpiryFilter filter = new SessionExpiryFilter(registry, entryPoint);

    @Test
    void expiredSessionReturnsUnifiedUnauthenticatedError() throws Exception {
        String sessionId = "expired-session-1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession(null, sessionId);
        request.setSession(session);

        SessionInformation info = new SessionInformation("principal", sessionId, new Date());
        info.expireNow();
        when(registry.getSessionInformation(sessionId)).thenReturn(info);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = ENTRY_POINT_MAPPER.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo(ErrorCode.AUTH_UNAUTHENTICATED.name());
        assertThat(body.has("message")).isTrue();

        // registry entry removed and the HttpSession invalidated
        Mockito.verify(registry).removeSessionInformation(sessionId);
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void validSessionPassesThroughFilterChain() throws Exception {
        String sessionId = "valid-session-1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession(null, sessionId));
        when(registry.getSessionInformation(sessionId)).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};
        filter.doFilter(request, response, (req, res) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
