package com.company.casehub.execution.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.dto.BulkAssignRequest;
import com.company.casehub.execution.dto.ProjectTestCaseResponse;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.service.ProjectTestPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/test-plan")
public class ProjectTestPlanController {

    private final ProjectTestPlanService service;

    public ProjectTestPlanController(ProjectTestPlanService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public List<ProjectTestCaseResponse> list(@PathVariable UUID projectId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return service.list(projectId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('project_test_case:add')")
    public ProjectTestCaseResponse add(@PathVariable UUID projectId, @RequestParam UUID masterTestCaseId,
                                       @RequestParam(defaultValue = "MANUAL") ProjectTestCaseSourceType source,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return service.addMasterCase(projectId, masterTestCaseId, source, principal);
    }

    @PostMapping("/{id}/remove")
    @PreAuthorize("hasAuthority('project_test_case:remove')")
    public ProjectTestCaseResponse remove(@PathVariable UUID projectId, @PathVariable UUID id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return service.remove(id, principal);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('project_test_case:restore')")
    public ProjectTestCaseResponse restore(@PathVariable UUID projectId, @PathVariable UUID id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return service.restore(id, principal);
    }

    @PostMapping("/{id}/assignees")
    @PreAuthorize("hasAuthority('project_test_case:assign')")
    public ProjectTestCaseResponse assign(@PathVariable UUID projectId, @PathVariable UUID id,
                                          @Valid @RequestBody AssigneeRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return service.assign(id, request, principal);
    }

    @PostMapping("/assignees/bulk")
    @PreAuthorize("hasAuthority('project_test_case:assign')")
    public List<ProjectTestCaseResponse> bulkAssign(@PathVariable UUID projectId,
                                                    @Valid @RequestBody BulkAssignRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return service.bulkAssign(request, principal);
    }
}
