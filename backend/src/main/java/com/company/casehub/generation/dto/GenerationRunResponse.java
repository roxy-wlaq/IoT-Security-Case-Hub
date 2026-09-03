package com.company.casehub.generation.dto;

import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.entity.GenerationRunMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GenerationRunResponse(UUID id, UUID projectId, GenerationRunMode mode,
                                    GenerationTriggerType triggerType, Instant executedAt,
                                    List<GenerationRecommendationResponse> recommendations) {
}
