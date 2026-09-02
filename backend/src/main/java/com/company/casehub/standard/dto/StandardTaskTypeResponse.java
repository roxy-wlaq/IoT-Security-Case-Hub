package com.company.casehub.standard.dto;

import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import java.time.Instant;
import java.util.UUID;

public record StandardTaskTypeResponse(
        UUID id,
        String code,
        String name,
        String type,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static StandardTaskTypeResponse from(StandardTaskTypeEntity entity) {
        return new StandardTaskTypeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getType(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
