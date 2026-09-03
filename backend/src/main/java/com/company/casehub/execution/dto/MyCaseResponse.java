package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.RelationStatus;
import java.time.Instant;
import java.util.UUID;

public record MyCaseResponse(UUID projectTestCaseId, UUID projectId, String projectNumber,
                             UUID masterTestCaseId, UUID testCaseVersionId, String caseCode,
                             ExecutionStatus executionStatus, RelationStatus relationStatus,
                             boolean removed, boolean assignedToMe, boolean newCase,
                             boolean readOnly, Instant firstViewedAt) {
}
