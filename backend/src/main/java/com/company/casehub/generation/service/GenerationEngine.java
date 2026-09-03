package com.company.casehub.generation.service;

import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleOutputEntity;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.generation.model.GenerationContext;
import com.company.casehub.generation.model.GenerationResult;
import com.company.casehub.generation.repository.GenerationRuleRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.entity.ProjectStandardEntity;
import com.company.casehub.project.service.CapabilityEngine;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ProgressiveRole;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GenerationEngine {

    private final GenerationRuleRepository ruleRepository;
    private final GroupEvaluator groupEvaluator;

    public GenerationEngine(GenerationRuleRepository ruleRepository, GroupEvaluator groupEvaluator) {
        this.ruleRepository = ruleRepository;
        this.groupEvaluator = groupEvaluator;
    }

    public GenerationResult evaluate(ProjectEntity project, GenerationRunMode mode,
                                     List<ProjectStandardEntity> projectStandards,
                                     CapabilityEngine capabilityEngine) {
        var standardIds = projectStandards.stream()
                .map(s -> s.getStandardTaskType().getId()).collect(java.util.stream.Collectors.toSet());
        GenerationContext context = new GenerationContext(project.getId(), mode, standardIds, capabilityEngine);
        Map<UUID, GenerationResult.Match> matches = new LinkedHashMap<>();
        for (GenerationRuleEntity rule : ruleRepository.findByStatusOrderByRuleCodeAsc(GenerationRuleStatus.ENABLED)) {
            if (!modeCompatible(rule.getMode(), mode) || !matchesRule(rule, context)) {
                continue;
            }
            for (GenerationRuleOutputEntity output : rule.getOutputs()) {
                TestCaseVersionEntity version = currentPublished(output.getMasterTestCase());
                if (version == null || (mode == GenerationRunMode.PROGRESSIVE_INITIAL
                        && version.getProgressiveRole() != ProgressiveRole.ENTRY)) {
                    continue;
                }
                matches.computeIfAbsent(output.getMasterTestCase().getId(), ignored ->
                        new GenerationResult.Match(output.getMasterTestCase(), version, new ArrayList<>()))
                        .matchingRules().add(rule);
            }
        }
        return new GenerationResult(null, new ArrayList<>(matches.values()));
    }

    private boolean matchesRule(GenerationRuleEntity rule, GenerationContext context) {
        return rule.getGroups().stream().filter(group -> group.getParent() == null)
                .findFirst().map(group -> groupEvaluator.matches(group, context)).orElse(false);
    }

    private boolean modeCompatible(GenerationRuleMode ruleMode, GenerationRunMode mode) {
        return mode == GenerationRunMode.FULL
                ? ruleMode == GenerationRuleMode.FULL || ruleMode == GenerationRuleMode.BOTH
                : ruleMode == GenerationRuleMode.PROGRESSIVE_INITIAL
                || ruleMode == GenerationRuleMode.BOTH;
    }

    private TestCaseVersionEntity currentPublished(MasterTestCaseEntity master) {
        return master.getVersions().stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElse(null);
    }
}
