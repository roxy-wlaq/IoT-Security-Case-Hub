package com.company.casehub.generation.dto;

import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.entity.GenerationRunMode;

public record GenerationRunRequest(GenerationRunMode mode, GenerationTriggerType triggerType) {
}
