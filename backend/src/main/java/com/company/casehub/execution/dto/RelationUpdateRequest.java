package com.company.casehub.execution.dto;

import com.company.casehub.execution.entity.RelationUpdateAction;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RelationUpdateRequest(@NotNull RelationUpdateAction action, UUID sourceDecisionPointId,
                                    UUID targetProjectTestCaseId) {
}
