package com.company.casehub.upgrade.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.upgrade.dto.VersionAvailabilityResponse;
import com.company.casehub.upgrade.dto.VersionUpgradeResponse;
import com.company.casehub.upgrade.service.VersionUpgradeService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/project-test-cases/{projectTestCaseId}/version")
public class VersionUpgradeController {
    private final VersionUpgradeService service;
    public VersionUpgradeController(VersionUpgradeService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('project_test_case:read')") public VersionAvailabilityResponse availability(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) { return service.availability(projectTestCaseId, principal); }
    @PostMapping("/keep") @PreAuthorize("hasAuthority('project:update')") public VersionUpgradeResponse keep(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) { return service.keep(projectTestCaseId, principal); }
    @PostMapping("/upgrade") @PreAuthorize("hasAuthority('project:update')") public VersionUpgradeResponse upgrade(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) { return service.upgrade(projectTestCaseId, principal); }
}
