package com.company.casehub.user.dto;

import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/v1/users/{id}/password-reset}. {@code password} is
 * optional: when omitted the backend generates a temporary password and
 * returns it once in {@code UserPasswordResetResponse#password()}.
 */
public record UserPasswordResetRequest(
        @Size(min = 1, max = 128)
        String password) {
}
