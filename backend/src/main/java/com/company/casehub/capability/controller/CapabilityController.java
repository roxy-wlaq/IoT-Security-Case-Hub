package com.company.casehub.capability.controller;

import com.company.casehub.capability.dto.CapabilityResponse;
import com.company.casehub.capability.dto.CapabilityTreeNode;
import com.company.casehub.capability.dto.CreateCapabilityRequest;
import com.company.casehub.capability.dto.UpdateCapabilityRequest;
import com.company.casehub.capability.service.CapabilityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Capability Library endpoints (frozen contract, Phase 5).
 *
 * <p>Reads are open to every authenticated user — {@code SecurityConfig} already
 * requires authentication for {@code anyRequest()}, so no extra annotation is needed.
 * Mutations are guarded with the frozen RBAC code {@code capability:manage_library}
 * (Security & RBAC Detail V1.0 §33, seeded by V002): ADMIN holds it, TESTER only
 * holds {@code capability:read}.
 */
@RestController
@RequestMapping("/api/v1/capabilities")
@RequiredArgsConstructor
public class CapabilityController {

    private final CapabilityService capabilityService;

    /** {@code GET /api/v1/capabilities/tree} */
    @GetMapping("/tree")
    public List<CapabilityTreeNode> tree() {
        return capabilityService.getTree();
    }

    /** {@code POST /api/v1/capabilities} */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('capability:manage_library')")
    public CapabilityResponse create(@Valid @RequestBody CreateCapabilityRequest request) {
        return capabilityService.create(request);
    }

    /** {@code PUT /api/v1/capabilities/{capabilityId}} */
    @PutMapping("/{capabilityId}")
    @PreAuthorize("hasAuthority('capability:manage_library')")
    public CapabilityResponse update(@PathVariable UUID capabilityId,
                                     @Valid @RequestBody UpdateCapabilityRequest request) {
        return capabilityService.update(capabilityId, request);
    }

    /** {@code POST /api/v1/capabilities/{capabilityId}/enable} */
    @PostMapping("/{capabilityId}/enable")
    @PreAuthorize("hasAuthority('capability:manage_library')")
    public CapabilityResponse enable(@PathVariable UUID capabilityId) {
        return capabilityService.enable(capabilityId);
    }

    /** {@code POST /api/v1/capabilities/{capabilityId}/disable} */
    @PostMapping("/{capabilityId}/disable")
    @PreAuthorize("hasAuthority('capability:manage_library')")
    public CapabilityResponse disable(@PathVariable UUID capabilityId) {
        return capabilityService.disable(capabilityId);
    }
}
