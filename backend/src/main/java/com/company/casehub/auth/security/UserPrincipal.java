package com.company.casehub.auth.security;

import com.company.casehub.user.entity.UserEntity;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal. Authorities are the user's effective permission codes
 * (so {@code @PreAuthorize("hasAuthority('user:read')")} works), while {@code roles}
 * is kept separately for UI/role checks. Identity equality is by {@code id} so the
 * {@code SessionRegistry} can match principals across sessions.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final Set<String> roles;
    private final Set<String> permissions;

    public UserPrincipal(UUID id, String username, String passwordHash, String displayName,
                         boolean enabled, boolean mustChangePassword, Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.enabled = enabled;
        this.mustChangePassword = mustChangePassword;
        this.roles = roles != null ? roles : new HashSet<>();
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    public static UserPrincipal from(UserEntity user, Set<String> roles, Set<String> permissions) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getDisplayName(), user.isEnabled(), user.isMustChangePassword(), roles, permissions);
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String p : permissions) {
            authorities.add(new SimpleGrantedAuthority(p));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPrincipal that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
