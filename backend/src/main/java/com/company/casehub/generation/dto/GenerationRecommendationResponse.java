package com.company.casehub.generation.dto;

import com.company.casehub.generation.entity.RecommendationStatus;
import java.util.List;
import java.util.UUID;

public record GenerationRecommendationResponse(UUID id, UUID runId, UUID masterTestCaseId,
                                               String caseCode, UUID resolvedVersionId,
                                               RecommendationStatus status,
                                               List<MatchedRule> recommendedBecause) {
    public record MatchedRule(UUID ruleId, String ruleCode, String ruleName) {
    }
}
