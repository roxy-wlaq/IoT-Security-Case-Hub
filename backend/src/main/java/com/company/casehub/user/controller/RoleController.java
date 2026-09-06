package com.company.casehub.user.controller;

import com.company.casehub.user.dto.RoleResponse;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role dictionary for the admin user-management UI. ADMIN + {@code user:read}
 * is enough to list roles (assignment itself needs {@code role:manage}).
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read') and (hasRole('ADMIN') or "
            + "(principal instanceof T(com.company.casehub.auth.security.UserPrincipal) "
            + "and principal.roles.contains('ADMIN')))")
    public List<RoleResponse> list() {
        return roleRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "code"))
                .stream()
                .map(RoleResponse::from)
                .toList();
    }
}
