package com.company.casehub.execution.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CompleteExecutionRequest(@NotEmpty List<UUID> selectedDecisionPointIds) {
}
