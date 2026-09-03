package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import java.util.UUID;

public record ExecutionStateResponse(UUID projectTestCaseId, ExecutionStatus executionStatus) {
}
