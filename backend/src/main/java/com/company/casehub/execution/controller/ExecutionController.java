package com.company.casehub.execution.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.execution.dto.CompleteExecutionRequest;
import com.company.casehub.execution.dto.ExecutionResponse;
import com.company.casehub.execution.dto.ExecutionStateResponse;
import com.company.casehub.execution.dto.ExecutionDetailResponse;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.service.ExecutionService;
import com.company.casehub.execution.service.RelationUpdateService;
import com.company.casehub.execution.dto.RelationUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/project-test-cases/{projectTestCaseId}/execution")
public class ExecutionController {
    private final ExecutionService service;
    private final RelationUpdateService relationUpdateService;
    public ExecutionController(ExecutionService service, RelationUpdateService relationUpdateService) { this.service = service; this.relationUpdateService = relationUpdateService; }
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('project_test_case:execute')")
    public ExecutionStateResponse start(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) {
        ProjectTestCaseEntity state = service.start(projectTestCaseId, principal);
        return new ExecutionStateResponse(state.getId(), state.getExecutionStatus());
    }
    @org.springframework.web.bind.annotation.GetMapping
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public ExecutionDetailResponse detail(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.detail(projectTestCaseId, principal);
    }
    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('project_test_case:execute')")
    public ExecutionResponse complete(@PathVariable UUID projectTestCaseId, @Valid @RequestBody CompleteExecutionRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) { return service.complete(projectTestCaseId, request, principal); }
    @PostMapping("/reopen")
    @PreAuthorize("hasAuthority('project_test_case:execute')")
    public ExecutionStateResponse reopen(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) {
        ProjectTestCaseEntity state = service.reopen(projectTestCaseId, principal);
        return new ExecutionStateResponse(state.getId(), state.getExecutionStatus());
    }
    @PostMapping("/relation")
    @PreAuthorize("hasAuthority('project_test_case:execute')")
    public void updateRelation(@PathVariable UUID projectTestCaseId, @Valid @RequestBody RelationUpdateRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) { relationUpdateService.update(projectTestCaseId, request, principal); }
}
