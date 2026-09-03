package com.company.casehub.project.dto;

import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID id,
        String projectNumber,
        String projectName,
        String deviceName,
        GenerationMode generationMode,
        ProjectStatus status,
        Instant createdAt) {
}
