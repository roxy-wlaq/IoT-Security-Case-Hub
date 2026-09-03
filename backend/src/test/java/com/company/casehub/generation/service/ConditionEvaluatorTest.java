package com.company.casehub.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationConditionEntity;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.model.GenerationContext;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.service.CapabilityEngine;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConditionEvaluatorTest {

    @Mock private CapabilityEngine capabilityEngine;

    @Test
    void evaluatesCapabilityOperatorsAndNonApplicableParent() {
        UUID projectId = UUID.randomUUID(); UUID capabilityId = UUID.randomUUID();
        CapabilityEntity capability = new CapabilityEntity(); capability.setId(capabilityId);
        GenerationContext context = new GenerationContext(projectId, com.company.casehub.generation.entity.GenerationRunMode.FULL, Set.of(), capabilityEngine);
        when(capabilityEngine.resolveEffectiveValue(projectId, capabilityId))
                .thenReturn(new CapabilityEngine.EffectiveCapability(ProjectCapabilityValue.YES, true, false));
        GenerationConditionEntity yes = condition(capability, GenerationOperator.EQ_YES);
        GenerationConditionEntity notNo = condition(capability, GenerationOperator.NE_NO);
        assertThat(new ConditionEvaluator().matches(yes, context)).isTrue();
        assertThat(new ConditionEvaluator().matches(notNo, context)).isTrue();
        when(capabilityEngine.resolveEffectiveValue(projectId, capabilityId))
                .thenReturn(new CapabilityEngine.EffectiveCapability(ProjectCapabilityValue.UNKNOWN, false, false));
        assertThat(new ConditionEvaluator().matches(yes, context)).isFalse();
    }

    private GenerationConditionEntity condition(CapabilityEntity capability, GenerationOperator operator) {
        GenerationConditionEntity condition = new GenerationConditionEntity();
        condition.setTargetType(ConditionTargetType.CAPABILITY); condition.setCapability(capability); condition.setOperator(operator);
        return condition;
    }
}
