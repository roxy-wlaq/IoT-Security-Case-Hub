package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.testcase.entity.SelectionMode;
import java.util.List;
import java.util.UUID;

public record ExecutionDetailResponse(UUID projectTestCaseId, UUID projectId, UUID masterTestCaseId, UUID customTestCaseId, UUID testCaseVersionId,
                                      ExecutionStatus executionStatus, SelectionMode selectionMode,
                                      boolean evidenceRequired, List<DecisionResponse> decisionPoints) {
    public record DecisionResponse(UUID id, int displayOrder, String name, String transitionType,
                                   List<UUID> targetMasterTestCaseIds, List<UUID> targetCustomTestCaseIds) { }
}
