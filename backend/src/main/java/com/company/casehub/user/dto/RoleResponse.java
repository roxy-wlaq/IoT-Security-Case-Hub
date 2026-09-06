package com.company.casehub.user.dto;

import com.company.casehub.user.entity.RoleEntity;

/**
 * {@code GET /api/v1/roles} — role dictionary for the user-management UI.
 */
public record RoleResponse(String code, String name, String description) {

    public static RoleResponse from(RoleEntity role) {
        return new RoleResponse(role.getCode(), role.getName(), role.getDescription());
    }
}
