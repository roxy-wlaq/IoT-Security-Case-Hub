package com.company.casehub.execution.dto;

import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectStatus;
import java.util.UUID;

public record MyProjectResponse(UUID id, String projectNumber, String projectName,
                                String deviceName, GenerationMode generationMode, ProjectStatus status) {
}
