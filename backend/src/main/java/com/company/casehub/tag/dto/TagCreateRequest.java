package com.company.casehub.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /api/v1/tags} request body. */
public record TagCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Boolean enabled) {
}
