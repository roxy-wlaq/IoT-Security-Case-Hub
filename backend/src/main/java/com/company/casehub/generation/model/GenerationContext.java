package com.company.casehub.generation.model;

import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.project.service.CapabilityEngine;
import java.util.Set;
import java.util.UUID;

public record GenerationContext(UUID projectId, GenerationRunMode mode, Set<UUID> standardTaskTypeIds,
                                CapabilityEngine capabilityEngine) {
}
