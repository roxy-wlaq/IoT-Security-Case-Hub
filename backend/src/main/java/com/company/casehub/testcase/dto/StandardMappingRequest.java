package com.company.casehub.testcase.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StandardMappingRequest(
        @NotNull UUID standardTaskTypeId,
        String mappingNote) {
}
