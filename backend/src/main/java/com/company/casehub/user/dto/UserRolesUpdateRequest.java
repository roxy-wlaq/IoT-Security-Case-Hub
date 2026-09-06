package com.company.casehub.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * {@code PUT /api/v1/users/{id}/roles} — replaces the full role set
 * (frozen contract: full replacement, not incremental add/remove).
 */
public record UserRolesUpdateRequest(
        @NotEmpty
        List<String> roles) {
}
