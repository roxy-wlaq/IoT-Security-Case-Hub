package com.company.casehub.user.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Single accessor for the currently authenticated {@link UserPrincipal}.
 * Business code must use this instead of touching {@code SecurityContextHolder}.
 */
@Slf4j
@Service
public class CurrentUserService {

    public UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new CaseHubException(ErrorCode.AUTH_UNAUTHENTICATED);
        }
        return principal;
    }

    public java.util.UUID currentUserId() {
        return currentUser().getId();
    }

    public boolean hasRole(String roleCode) {
        return currentUser().getRoles().contains(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        return currentUser().getPermissions().contains(permissionCode);
    }
}
