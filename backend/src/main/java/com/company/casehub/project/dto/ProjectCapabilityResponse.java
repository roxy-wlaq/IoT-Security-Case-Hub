package com.company.casehub.project.dto;

import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import java.time.Instant;
import java.util.UUID;

public record ProjectCapabilityResponse(
        UUID capabilityId,
        UUID parentId,
        String code,
        String name,
        ProjectCapabilityValue value,
        ProjectCapabilitySource source,
        boolean derived,
        String comment,
        Instant updatedAt) {
}
