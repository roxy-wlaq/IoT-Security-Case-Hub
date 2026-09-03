package com.company.casehub.project.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.project.dto.ProjectCapabilityRequest;
import com.company.casehub.project.dto.ProjectCapabilityResponse;
import com.company.casehub.project.service.ProjectCapabilityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/capabilities")
public class ProjectCapabilityController {

    private final ProjectCapabilityService service;

    public ProjectCapabilityController(ProjectCapabilityService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('project_capability:read')")
    public List<ProjectCapabilityResponse> list(@PathVariable UUID projectId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return service.list(projectId, principal);
    }

    @PutMapping("/{capabilityId}")
    @PreAuthorize("hasAuthority('project_capability:update')")
    public ProjectCapabilityResponse update(@PathVariable UUID projectId, @PathVariable UUID capabilityId,
                                            @Valid @RequestBody ProjectCapabilityRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return service.setValue(projectId, capabilityId, request, principal);
    }
}
