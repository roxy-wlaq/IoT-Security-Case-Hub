package com.company.casehub.tool.dto;

import com.company.casehub.tool.entity.ToolEntity;
import java.time.Instant;
import java.util.UUID;

public record ToolResponse(
        UUID id,
        String code,
        String name,
        String description,
        String platform,
        String website,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static ToolResponse from(ToolEntity entity) {
        return new ToolResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPlatform(),
                entity.getWebsite(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
