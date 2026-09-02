package com.company.casehub.category.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code PUT /api/v1/categories/{id}} request body. Every field is optional;
 * {@code null} means "leave unchanged". As with create, {@code level} is derived
 * server-side from {@code parentId} when a parent is supplied.
 */
public record CategoryUpdateRequest(
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        UUID parentId,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Integer sortOrder,

        Boolean enabled) {
}
