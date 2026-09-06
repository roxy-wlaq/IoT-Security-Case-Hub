package com.company.casehub.user.dto;

import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import java.util.List;
import java.util.UUID;

/**
 * {@code POST /api/v1/users} response. {@code generatedPassword} is present
 * only when the backend generated the initial password; it is returned once
 * and never persisted in plaintext.
 */
public record UserCreatedResponse(
        UUID id,
        String username,
        String displayName,
        boolean enabled,
        boolean mustChangePassword,
        List<String> roles,
        String generatedPassword) {

    public static UserCreatedResponse of(UserEntity user, List<RoleEntity> roles, String generatedPassword) {
        return new UserCreatedResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isEnabled(),
                user.isMustChangePassword(),
                roles.stream().map(RoleEntity::getCode).sorted().toList(),
                generatedPassword);
    }
}
