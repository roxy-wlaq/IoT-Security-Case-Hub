package com.company.casehub.tag.dto;

import com.company.casehub.tag.entity.TagEntity;
import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static TagResponse from(TagEntity entity) {
        return new TagResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
