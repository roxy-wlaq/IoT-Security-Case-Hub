package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.RelationStatus;
import java.util.List;
import java.util.UUID;

public record ProjectLogicGraphResponse(List<Node> nodes, List<Edge> edges) {
    public record Node(UUID projectTestCaseId, UUID masterTestCaseId, UUID customTestCaseId, String caseCode, UUID testCaseVersionId,
                       ExecutionStatus executionStatus, RelationStatus relationStatus, boolean root,
                       List<String> assignees) { }
    public record Edge(UUID id, UUID sourceProjectTestCaseId, UUID targetProjectTestCaseId,
                       UUID sourceDecisionPointId, String label) { }
}
