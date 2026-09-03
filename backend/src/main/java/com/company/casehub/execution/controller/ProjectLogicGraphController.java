package com.company.casehub.execution.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.execution.dto.ProjectLogicGraphResponse;
import com.company.casehub.execution.service.ProjectLogicGraphService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/logic-graph")
public class ProjectLogicGraphController {
    private final ProjectLogicGraphService service;
    public ProjectLogicGraphController(ProjectLogicGraphService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public ProjectLogicGraphResponse graph(@PathVariable UUID projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.graph(projectId, principal);
    }
}
