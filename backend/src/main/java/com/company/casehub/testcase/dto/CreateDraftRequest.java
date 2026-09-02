package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.ProgressiveRole;
import com.company.casehub.testcase.entity.SelectionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateDraftRequest(
        @NotBlank @Size(max = 100) String caseCode,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 255) String caseName,
        String testPurpose,
        String preconditions,
        @NotNull SelectionMode selectionMode,
        Boolean evidenceRequired,
        String evidenceRequirement,
        String remarkRequirement,
        ProgressiveRole progressiveRole,
        @Valid List<StepRequest> steps,
        List<UUID> tagIds,
        List<UUID> toolIds,
        @Valid List<StandardMappingRequest> standardMappings) {
}
