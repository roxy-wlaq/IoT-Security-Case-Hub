package com.company.casehub.customcase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomStepRequest(@Size(max = 255) String title, @NotBlank String content) {
}
