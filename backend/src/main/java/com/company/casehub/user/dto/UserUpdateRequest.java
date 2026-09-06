package com.company.casehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/v1/users/{id}} — the only editable profile field in V1 is
 * the display name; username is immutable.
 */
public record UserUpdateRequest(
        @NotBlank
        @Size(max = 150)
        String displayName) {
}
