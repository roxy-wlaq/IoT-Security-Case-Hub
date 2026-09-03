package com.company.casehub.project.dto;

import com.company.casehub.project.entity.GenerationMode;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ProjectUpdateRequest(
        @NotBlank String projectName,
        @NotBlank String deviceName,
        GenerationMode generationMode,
        List<UUID> standardTaskTypeIds,
        UUID primaryCoordinatorId) {
}
