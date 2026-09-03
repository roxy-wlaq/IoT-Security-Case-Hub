package com.company.casehub.generation.model;

import com.company.casehub.generation.entity.GenerationRunEntity;
import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import java.util.List;
import java.util.List;

public record GenerationResult(GenerationRunEntity run, List<Match> matches) {
    public record Match(MasterTestCaseEntity masterTestCase, TestCaseVersionEntity version,
                        List<GenerationRuleEntity> matchingRules) {
    }
}
