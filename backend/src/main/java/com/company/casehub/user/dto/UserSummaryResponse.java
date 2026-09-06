package com.company.casehub.user.dto;

import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import java.util.List;
import java.util.UUID;

/**
 * Frozen user-management list/contract item: {@code GET /api/v1/users}.
 * Never exposes password hashes or audit metadata.
 */
public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        boolean enabled,
        boolean mustChangePassword,
        List<String> roles) {

    public static UserSummaryResponse from(UserEntity user, List<RoleEntity> roles) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isEnabled(),
                user.isMustChangePassword(),
                roles.stream().map(RoleEntity::getCode).sorted().toList());
    }
}
