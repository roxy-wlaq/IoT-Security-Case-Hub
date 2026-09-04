package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.entity.RelationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectTestCaseResponse(
        UUID id,
        UUID projectId,
        UUID masterTestCaseId,
        UUID customTestCaseId,
        UUID testCaseVersionId,
        String caseCode,
        ExecutionStatus executionStatus,
        RelationStatus relationStatus,
        boolean removed,
        List<ProjectTestCaseSourceType> sources,
        List<AssigneeResponse> assignees,
        Instant lastModifiedAt) {

    public record AssigneeResponse(UUID userId, String username, String displayName, Instant firstViewedAt) {
    }
}
