package com.company.casehub.project.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.dto.ProjectResponse;
import com.company.casehub.project.dto.ProjectSummaryResponse;
import com.company.casehub.project.dto.ProjectUpdateRequest;
import com.company.casehub.project.entity.ProjectStatus;
import com.company.casehub.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('project:read')")
    public List<ProjectSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.list(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('project:create')")
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(request, principal);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('project:read')")
    public ProjectResponse get(@PathVariable UUID projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(projectId, principal);
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody ProjectUpdateRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(projectId, request, principal);
    }

    @PutMapping("/{projectId}/status")
    @PreAuthorize("hasAuthority('project:update') or hasAuthority('project:archive') or hasAuthority('project:complete')")
    public ProjectResponse status(@PathVariable UUID projectId, @RequestBody ProjectStatus status,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return service.changeStatus(projectId, status, principal);
    }
}
