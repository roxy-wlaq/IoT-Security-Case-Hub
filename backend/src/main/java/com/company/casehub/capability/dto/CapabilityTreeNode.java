package com.company.casehub.capability.dto;

import com.company.casehub.capability.entity.CapabilityEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A node of the Capability Tree as returned by {@code GET /api/v1/capabilities/tree}.
 *
 * <p>Arbitrary depth is supported (for example {@code Bluetooth -> BLE -> Pairing}).
 * {@code children} is never {@code null}: a leaf node carries an empty list so the
 * front-end can render the tree without null checks.
 */
public record CapabilityTreeNode(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean enabled,
        List<CapabilityTreeNode> children) {

    public static CapabilityTreeNode of(CapabilityEntity entity, List<CapabilityTreeNode> children) {
        return new CapabilityTreeNode(
                entity.getId(),
                entity.getParentId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.isEnabled(),
                children);
    }

    public static CapabilityTreeNode leaf(CapabilityEntity entity) {
        return of(entity, new ArrayList<>());
    }
}
