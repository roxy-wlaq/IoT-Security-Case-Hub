package com.company.casehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code POST /api/v1/users}. {@code password} is optional: when omitted the
 * backend generates one and returns it exactly once in
 * {@code UserCreatedResponse.generatedPassword}.
 */
public record UserCreateRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$", message = "Username may contain letters, digits, dot, underscore and hyphen and must start with a letter or digit.")
        String username,

        @NotBlank
        @Size(max = 150)
        String displayName,

        @Size(min = 1, max = 128)
        String password,

        @NotEmpty
        List<String> roles) {
}
