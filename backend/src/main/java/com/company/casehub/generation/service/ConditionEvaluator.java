package com.company.casehub.generation.service;

import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationConditionEntity;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.model.GenerationContext;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import org.springframework.stereotype.Component;

@Component
public class ConditionEvaluator {

    public boolean matches(GenerationConditionEntity condition, GenerationContext context) {
        if (condition.getTargetType() == ConditionTargetType.STANDARD_TASK_TYPE) {
            return context.standardTaskTypeIds().contains(condition.getStandardTaskType().getId());
        }
        var effective = context.capabilityEngine().resolveEffectiveValue(
                context.projectId(), condition.getCapability().getId());
        if (!effective.applicable()) {
            return false;
        }
        ProjectCapabilityValue value = effective.value();
        return switch (condition.getOperator()) {
            case EQ_YES -> value == ProjectCapabilityValue.YES;
            case EQ_NO -> value == ProjectCapabilityValue.NO;
            case EQ_UNKNOWN -> value == ProjectCapabilityValue.UNKNOWN;
            case NE_NO -> value != ProjectCapabilityValue.NO;
            case NE_YES -> value != ProjectCapabilityValue.YES;
            case PRESENT, ANY -> value != ProjectCapabilityValue.UNKNOWN;
        };
    }
}
