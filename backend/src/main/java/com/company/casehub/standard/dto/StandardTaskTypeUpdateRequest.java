package com.company.casehub.standard.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/v1/standard-task-types/{id}} request body. Every field is optional;
 * {@code null} means "leave unchanged".
 */
public record StandardTaskTypeUpdateRequest(
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Pattern(regexp = "STANDARD|TASK_TYPE", message = "type must be STANDARD or TASK_TYPE")
        String type,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Boolean enabled) {
}
