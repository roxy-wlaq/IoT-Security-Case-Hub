package com.company.casehub.testcase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StepRequest(
        @Size(max = 255) String title,
        @NotBlank String content) {
}
