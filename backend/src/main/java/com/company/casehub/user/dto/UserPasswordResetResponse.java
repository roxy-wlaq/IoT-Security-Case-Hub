package com.company.casehub.user.dto;

/**
 * {@code POST /api/v1/users/{id}/password-reset} response. The temporary
 * password is returned ONLY when the backend generated it ({@code generated}
 * = true); admin-supplied passwords are never echoed back. The target user is
 * forced to change the password at next login ({@code mustChangePassword}).
 */
public record UserPasswordResetResponse(
        boolean generated,
        String password,
        boolean mustChangePassword) {
}
