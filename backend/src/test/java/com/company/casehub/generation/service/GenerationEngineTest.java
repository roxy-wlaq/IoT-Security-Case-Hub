package com.company.casehub.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.casehub.generation.entity.GenerationConditionGroupEntity;
import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleOutputEntity;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.generation.repository.GenerationRuleRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.entity.ProjectStandardEntity;
import com.company.casehub.project.service.CapabilityEngine;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ProgressiveRole;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerationEngineTest {

    @Mock private GenerationRuleRepository ruleRepository;
    @Mock private GroupEvaluator groupEvaluator;
    @Mock private CapabilityEngine capabilityEngine;

    @Test
    void deduplicatesMasterAndRetainsAllMatchingRulesAndFiltersProgressiveNormal() {
        UUID masterId = UUID.randomUUID();
        MasterTestCaseEntity master = master(masterId, ProgressiveRole.ENTRY);
        GenerationRuleEntity first = rule("RULE-A", GenerationRuleMode.BOTH, master);
        GenerationRuleEntity second = rule("RULE-B", GenerationRuleMode.FULL, master);
        when(ruleRepository.findByStatusOrderByRuleCodeAsc(GenerationRuleStatus.ENABLED)).thenReturn(List.of(first, second));
        when(groupEvaluator.matches(org.mockito.ArgumentMatchers.any(GenerationConditionGroupEntity.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        ProjectEntity project = new ProjectEntity(); project.setId(UUID.randomUUID());

        GenerationEngine engine = new GenerationEngine(ruleRepository, groupEvaluator);
        var result = engine.evaluate(project, GenerationRunMode.FULL, List.of(), capabilityEngine);
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).matchingRules()).extracting(GenerationRuleEntity::getRuleCode)
                .containsExactly("RULE-A", "RULE-B");
    }

    private GenerationRuleEntity rule(String code, GenerationRuleMode mode, MasterTestCaseEntity master) {
        GenerationRuleEntity rule = new GenerationRuleEntity(); rule.setRuleCode(code); rule.setMode(mode);
        GenerationConditionGroupEntity group = new GenerationConditionGroupEntity(); group.setRule(rule);
        rule.getGroups().add(group);
        GenerationRuleOutputEntity output = new GenerationRuleOutputEntity(); output.setRule(rule); output.setMasterTestCase(master);
        rule.getOutputs().add(output);
        return rule;
    }

    private MasterTestCaseEntity master(UUID id, ProgressiveRole role) {
        MasterTestCaseEntity master = new MasterTestCaseEntity(); master.setId(id);
        TestCaseVersionEntity version = new TestCaseVersionEntity(); version.setId(UUID.randomUUID());
        version.setMasterTestCase(master); version.setCurrentVersion(true); version.setStatus(TestCaseVersionStatus.PUBLISHED);
        version.setProgressiveRole(role); version.setSelectionMode(SelectionMode.SINGLE); version.setCaseName("case");
        master.getVersions().add(version);
        return master;
    }
}
