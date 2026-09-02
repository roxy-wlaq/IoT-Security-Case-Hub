package com.company.casehub.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code POST /api/v1/categories} request body.
 *
 * <p>There is deliberately no {@code level} field: the server derives the level from
 * {@code parentId} and never trusts a client-supplied value.
 */
public record CategoryCreateRequest(
        @Size(max = 100, message = "code must be at most 100 characters")
        @NotBlank(message = "code is required")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        /** {@code null} creates a level-1 category; a level-1 parent id creates a level-2 category. */
        UUID parentId,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Integer sortOrder,

        Boolean enabled) {
}
