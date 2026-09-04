package com.company.casehub.change.dto;

import com.company.casehub.project.entity.ProjectCapabilityValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CapabilityUpdateRequestPayload(@NotNull ProjectCapabilityValue proposedValue,
                                              @NotBlank String reason, String evidenceReference) { }
