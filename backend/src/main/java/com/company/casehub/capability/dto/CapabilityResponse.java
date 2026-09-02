package com.company.casehub.capability.dto;

import com.company.casehub.capability.entity.CapabilityEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Flat projection of a capability node. Used by the create / update responses.
 *
 * @param parentId {@code null} for a root capability.
 */
public record CapabilityResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static CapabilityResponse of(CapabilityEntity entity) {
        return new CapabilityResponse(
                entity.getId(),
                entity.getParentId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
