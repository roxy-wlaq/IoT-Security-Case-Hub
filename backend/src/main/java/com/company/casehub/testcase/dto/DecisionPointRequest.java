package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TransitionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record DecisionPointRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @NotNull @Min(1) Integer displayOrder,
        @NotNull TransitionType transitionType,
        List<UUID> targetMasterTestCaseIds) {
}
