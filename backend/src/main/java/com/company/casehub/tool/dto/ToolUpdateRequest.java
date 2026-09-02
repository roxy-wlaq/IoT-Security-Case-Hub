package com.company.casehub.tool.dto;

import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/v1/tools/{id}} request body. Every field is optional;
 * {@code null} means "leave unchanged".
 */
public record ToolUpdateRequest(
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @Size(max = 100, message = "platform must be at most 100 characters")
        String platform,

        @Size(max = 500, message = "website must be at most 500 characters")
        String website,

        Boolean enabled) {
}
