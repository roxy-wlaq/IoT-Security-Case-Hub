package com.company.casehub.customcase.dto;

import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomTestCaseResponse(
        UUID id, UUID projectId, String caseCode, String caseName, String testPurpose, String preconditions,
        SelectionMode selectionMode, boolean evidenceRequired, String evidenceRequirement, String remarkRequirement,
        UUID projectTestCaseId, UUID createdBy, List<StepResponse> steps, List<DecisionPointResponse> decisionPoints,
        Instant createdAt, Instant updatedAt) {
    public record StepResponse(UUID id, int sequenceNo, String title, String content) { }
    public record DecisionPointResponse(UUID id, int displayOrder, String name, String description,
                                        TransitionType transitionType, List<TargetResponse> targets) { }
    public record TargetResponse(UUID masterTestCaseId, UUID customTestCaseId, String caseCode) { }
}
