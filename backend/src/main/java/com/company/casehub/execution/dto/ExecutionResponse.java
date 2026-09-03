package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.testcase.entity.TransitionType;
import java.util.List;
import java.util.UUID;

public record ExecutionResponse(UUID projectTestCaseId, ExecutionStatus executionStatus,
                                List<UUID> selectedDecisionPointIds,
                                List<BranchOutcomeResponse> branchOutcomes,
                                List<UUID> affectedTargetProjectTestCaseIds) {
    public record BranchOutcomeResponse(UUID decisionPointId, TransitionType transitionType, UUID targetMasterTestCaseId) { }
}
