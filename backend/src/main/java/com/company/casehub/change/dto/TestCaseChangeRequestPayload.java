package com.company.casehub.change.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TestCaseChangeRequestPayload(@NotNull UUID sourceVersionId, @NotBlank String reason) { }
