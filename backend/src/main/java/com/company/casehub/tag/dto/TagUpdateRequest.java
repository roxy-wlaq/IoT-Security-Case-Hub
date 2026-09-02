package com.company.casehub.tag.dto;

import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/v1/tags/{id}} request body. Every field is optional;
 * {@code null} means "leave unchanged".
 */
public record TagUpdateRequest(
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Boolean enabled) {
}
