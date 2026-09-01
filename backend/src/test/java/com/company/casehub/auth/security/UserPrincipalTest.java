package com.company.casehub.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserPrincipalTest {

    private UserPrincipal principal(String id, Set<String> roles, Set<String> permissions) {
        return new UserPrincipal(UUID.fromString(id), "alice", "hash", "Alice",
                true, false, roles, permissions);
    }

    @Test
    void authoritiesArePermissionCodes() {
        UserPrincipal p = principal("00000000-0000-0000-0000-000000000001",
                Set.of("ADMIN"), Set.of("user:read", "project:create"));
        assertThat(p.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("user:read", "project:create");
    }

    @Test
    void equalityAndHashCodeByUserId() {
        UUID id = UUID.randomUUID();
        UserPrincipal a = new UserPrincipal(id, "alice", "h", "Alice", true, false, Set.of(), Set.of());
        UserPrincipal b = new UserPrincipal(id, "alice2", "h2", "Alice2", false, true, Set.of("X"), Set.of("Y"));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void differentIdsAreNotEqual() {
        UserPrincipal a = principal("00000000-0000-0000-0000-00000000000a", Set.of(), Set.of());
        UserPrincipal b = principal("00000000-0000-0000-0000-00000000000b", Set.of(), Set.of());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void disabledUserIsNotEnabled() {
        UserPrincipal p = principal("00000000-0000-0000-0000-00000000000c", Set.of(), Set.of());
        assertThat(p.isEnabled()).isTrue();
        UserPrincipal disabled = new UserPrincipal(UUID.randomUUID(), "bob", "h", "Bob",
                false, false, Set.of(), Set.of());
        assertThat(disabled.isEnabled()).isFalse();
    }
}
