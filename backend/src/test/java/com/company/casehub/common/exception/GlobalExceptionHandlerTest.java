package com.company.casehub.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.company.casehub.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsCaseHubExceptionToFrozenCodeAndStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ApiErrorResponse> response =
                handler.handleCaseHub(new CaseHubException(ErrorCode.AUTH_INVALID_CREDENTIALS), request);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    void carriesStructuredDetails() {
        CaseHubException ex = new CaseHubException(ErrorCode.CONFLICT, "dup")
                .withDetail("username", "alice");
        ResponseEntity<ApiErrorResponse> response = handler.handleCaseHub(ex, new MockHttpServletRequest());
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().details()).containsKey("username");
    }

    @Test
    void mapsAccessDeniedTo403() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("no"), new MockHttpServletRequest());
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("AUTH_UNAUTHORIZED");
    }

    @Test
    void mapsAuthenticationExceptionTo401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAuthentication(
                new AuthenticationException("bad") {}, new MockHttpServletRequest());
        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("AUTH_UNAUTHENTICATED");
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingDetail() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("secret SQL: xxx"), new MockHttpServletRequest());
        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("secret SQL");
    }
}
