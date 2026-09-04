package com.company.casehub.customcase.dto;

import com.company.casehub.testcase.entity.SelectionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CustomTestCaseRequest(
        @NotBlank @Size(max = 100) String caseCode,
        @NotBlank @Size(max = 255) String caseName,
        String testPurpose,
        String preconditions,
        @NotNull SelectionMode selectionMode,
        Boolean evidenceRequired,
        String evidenceRequirement,
        String remarkRequirement,
        @Valid List<CustomStepRequest> steps,
        @Valid List<CustomDecisionPointRequest> decisionPoints) {
}
