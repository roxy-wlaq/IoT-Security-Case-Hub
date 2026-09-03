package com.company.casehub.execution.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssigneeRequest(@NotNull UUID userId) {
}
