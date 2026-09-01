package com.company.casehub.auth.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String displayName,
        boolean enabled,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions) {
}
