package com.company.casehub.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Set<String> roles, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "alice", "hash", "Alice",
                true, false, roles, permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        principal.getAuthorities()));
    }

    @Test
    void exposesPrincipalFields() {
        UUID id = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(id, "alice", "hash", "Alice",
                true, true, Set.of("ADMIN"), Set.of("user:read", "audit:read"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(service.currentUserId()).isEqualTo(id);
        assertThat(service.hasRole("ADMIN")).isTrue();
        assertThat(service.hasPermission("user:read")).isTrue();
        assertThat(service.hasPermission("project:create")).isFalse();
    }

    @Test
    void unauthenticatedThrows() {
        assertThatThrownBy(() -> service.currentUser())
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHENTICATED);
    }

    @Test
    void roleAndPermissionChecks() {
        authenticate(Set.of("TESTER"), Set.of("project:read", "test_case:read"));
        assertThat(service.hasRole("TESTER")).isTrue();
        assertThat(service.hasRole("ADMIN")).isFalse();
        assertThat(service.hasPermission("test_case:read")).isTrue();
        assertThat(service.hasPermission("user:read")).isFalse();
    }
}
