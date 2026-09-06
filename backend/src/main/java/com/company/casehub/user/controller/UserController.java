package com.company.casehub.user.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.user.dto.UserCreateRequest;
import com.company.casehub.user.dto.UserCreatedResponse;
import com.company.casehub.user.dto.UserPasswordResetRequest;
import com.company.casehub.user.dto.UserPasswordResetResponse;
import com.company.casehub.user.dto.UserRolesUpdateRequest;
import com.company.casehub.user.dto.UserSummaryResponse;
import com.company.casehub.user.dto.UserUpdateRequest;
import com.company.casehub.user.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin user-management endpoints (frozen contract).
 *
 * <p>Every endpoint requires the ADMIN role AND the matching frozen permission
 * from V002 ({@code user:read} / {@code user:create} / {@code user:update} /
 * {@code user:disable} / {@code role:manage}). Roles are checked against
 * {@code principal.roles} because {@link UserPrincipal} exposes permission
 * codes — not ROLE_* authorities — as its granted authorities.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String ADMIN_AND =
            "(hasRole('ADMIN') or (principal instanceof T(com.company.casehub.auth.security.UserPrincipal) "
                    + "and principal.roles.contains('ADMIN')))";

    private final UserManagementService service;

    public UserController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("(hasAuthority('user:read') and " + ADMIN_AND + ")")
    public PagedResponse<UserSummaryResponse> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(q, enabled, role, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("(hasAuthority('user:create') and " + ADMIN_AND + ")")
    public UserCreatedResponse create(@Valid @RequestBody UserCreateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasAuthority('user:update') and " + ADMIN_AND + ")")
    public UserSummaryResponse update(@PathVariable("id") UUID id,
                                      @Valid @RequestBody UserUpdateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(id, request, principal);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("(hasAuthority('role:manage') and " + ADMIN_AND + ")")
    public UserSummaryResponse updateRoles(@PathVariable("id") UUID id,
                                           @Valid @RequestBody UserRolesUpdateRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return service.updateRoles(id, request, principal);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("(hasAuthority('user:disable') and " + ADMIN_AND + ")")
    public UserSummaryResponse enable(@PathVariable("id") UUID id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return service.setEnabled(id, true, principal);
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("(hasAuthority('user:disable') and " + ADMIN_AND + ")")
    public UserSummaryResponse disable(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return service.setEnabled(id, false, principal);
    }

    @PostMapping("/{id}/password-reset")
    @PreAuthorize("(hasAuthority('user:update') and " + ADMIN_AND + ")")
    public UserPasswordResetResponse resetPassword(@PathVariable("id") UUID id,
                                                   @Valid @RequestBody UserPasswordResetRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return service.resetPassword(id, request, principal);
    }
}
