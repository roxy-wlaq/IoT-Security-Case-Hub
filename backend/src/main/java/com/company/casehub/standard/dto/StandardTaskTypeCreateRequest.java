package com.company.casehub.standard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/v1/standard-task-types} request body.
 *
 * <p>{@code type} is validated here with Bean Validation; the DB CHECK constraint on
 * {@code standard_task_types.type} is the second line of defence.
 */
public record StandardTaskTypeCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must be at most 100 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @NotBlank(message = "type is required")
        @Pattern(regexp = "STANDARD|TASK_TYPE", message = "type must be STANDARD or TASK_TYPE")
        String type,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Boolean enabled) {
}
