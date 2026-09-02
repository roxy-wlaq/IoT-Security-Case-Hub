package com.company.casehub.capability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/capabilities}.
 *
 * @param parentId {@code null} (or omitted) creates a root capability.
 * @param sortOrder ordering among siblings; {@code null} means "use the default 0".
 */
public record CreateCapabilityRequest(
        UUID parentId,

        @NotBlank(message = "code is required")
        @Size(max = 120, message = "code must be at most 120 characters")
        @Pattern(regexp = "^[^\\s]+$", message = "code must not contain whitespace")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 180, message = "name must be at most 180 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        Integer sortOrder) {
}
