package com.company.casehub.generation.service;

import com.company.casehub.generation.entity.GenerationConditionGroupEntity;
import com.company.casehub.generation.entity.GroupOperator;
import com.company.casehub.generation.model.GenerationContext;
import org.springframework.stereotype.Component;

@Component
public class GroupEvaluator {

    private final ConditionEvaluator conditionEvaluator;

    public GroupEvaluator(ConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    public boolean matches(GenerationConditionGroupEntity group, GenerationContext context) {
        var results = group.getConditions().stream().map(condition -> conditionEvaluator.matches(condition, context)).toList();
        boolean own = group.getLogicOperator() == GroupOperator.AND
                ? results.stream().allMatch(Boolean::booleanValue)
                : results.stream().anyMatch(Boolean::booleanValue);
        if (group.getParent() != null) {
            return own;
        }
        var childResults = group.getRule().getGroups().stream()
                .filter(candidate -> candidate.getParent() == group)
                .map(child -> matches(child, context)).toList();
        if (childResults.isEmpty()) {
            return own;
        }
        return group.getLogicOperator() == GroupOperator.AND
                ? own && childResults.stream().allMatch(Boolean::booleanValue)
                : own || childResults.stream().anyMatch(Boolean::booleanValue);
    }
}
