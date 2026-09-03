package com.company.casehub.project.dto;

import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;

public record ProjectCapabilityRequest(
        ProjectCapabilityValue value,
        ProjectCapabilitySource source,
        String comment) {
}
