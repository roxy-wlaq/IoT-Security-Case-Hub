package com.company.casehub.project.dto;

import com.company.casehub.project.entity.GenerationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ProjectCreateRequest(
        @NotBlank String projectName,
        @NotBlank String deviceName,
        GenerationMode generationMode,
        @NotEmpty List<UUID> standardTaskTypeIds,
        UUID primaryCoordinatorId) {
}
