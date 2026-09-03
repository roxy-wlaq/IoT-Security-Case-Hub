package com.company.casehub.generation.dto;

import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GroupOperator;
import java.util.List;
import java.util.UUID;

public record GenerationRuleResponse(
        UUID id,
        String ruleCode,
        String name,
        String description,
        GenerationRuleMode mode,
        GenerationRuleStatus status,
        List<GroupResponse> groups,
        List<UUID> outputMasterTestCaseIds) {

    public record GroupResponse(UUID id, UUID parentGroupId, GroupOperator logicOperator,
                                int sortOrder, List<ConditionResponse> conditions) {
    }

    public record ConditionResponse(UUID id, ConditionTargetType targetType, UUID capabilityId,
                                    UUID standardTaskTypeId, GenerationOperator operator, int sortOrder) {
    }
}
