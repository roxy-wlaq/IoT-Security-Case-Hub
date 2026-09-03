package com.company.casehub.project.dto;

import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String projectNumber,
        String projectName,
        String deviceName,
        GenerationMode generationMode,
        ProjectStatus status,
        UUID createdBy,
        List<UUID> standardTaskTypeIds,
        List<CoordinatorResponse> coordinators,
        Instant createdAt,
        Instant updatedAt) {

    public record CoordinatorResponse(UUID userId, String username, String displayName, boolean primary) {
    }
}
