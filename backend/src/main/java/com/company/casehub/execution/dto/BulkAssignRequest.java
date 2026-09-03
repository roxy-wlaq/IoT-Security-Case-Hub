package com.company.casehub.execution.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkAssignRequest(@NotEmpty List<UUID> projectTestCaseIds, @NotEmpty List<UUID> userIds) {
}
