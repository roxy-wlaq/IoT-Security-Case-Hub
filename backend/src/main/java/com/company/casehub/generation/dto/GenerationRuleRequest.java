package com.company.casehub.generation.dto;

import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GroupOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record GenerationRuleRequest(
        @NotBlank String ruleCode,
        @NotBlank String name,
        String description,
        GenerationRuleMode mode,
        GenerationRuleStatus status,
        @NotEmpty List<@Valid GroupRequest> groups,
        @NotEmpty List<UUID> outputMasterTestCaseIds) {

    public record GroupRequest(
            Integer parentGroupIndex,
            GroupOperator logicOperator,
            int sortOrder,
            List<@Valid ConditionRequest> conditions) {
    }

    public record ConditionRequest(
            ConditionTargetType targetType,
            UUID capabilityId,
            UUID standardTaskTypeId,
            GenerationOperator operator,
            int sortOrder) {
    }
}
