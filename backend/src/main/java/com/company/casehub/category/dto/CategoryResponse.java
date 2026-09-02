package com.company.casehub.category.dto;

import com.company.casehub.category.entity.CategoryEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Category payload. {@code children} is populated by the {@code /categories/tree}
 * endpoint; create/update return the node with an empty child list.
 */
public record CategoryResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        int level,
        String description,
        int sortOrder,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        List<CategoryResponse> children) {

    public static CategoryResponse from(CategoryEntity entity) {
        return from(entity, List.of());
    }

    public static CategoryResponse from(CategoryEntity entity, List<CategoryResponse> children) {
        CategoryEntity parent = entity.getParent();
        return new CategoryResponse(
                entity.getId(),
                parent == null ? null : parent.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getLevel(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                children);
    }
}
